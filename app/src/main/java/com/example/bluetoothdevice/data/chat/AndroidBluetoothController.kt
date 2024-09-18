package com.example.bluetoothdevice.data.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.bluetoothdevice.domain.chat.BluetoothController
import com.example.bluetoothdevice.domain.chat.BluetoothDeviceDomain
import com.example.bluetoothdevice.domain.chat.BluetoothMessage
import com.example.bluetoothdevice.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    //Ini yang handling semua urusan perblututan, gausah ditanya soalnya di modif dikit langsung buyar ini

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null


    //terkoneksi ama pasangan yang benar (paired device)
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    //cuman nampilin perangkat yang terhubung saat itu
    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()


    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        val deviceName = device.name ?: return@FoundDeviceReceiver
        // Filter specific devices by name
        if (deviceName == "ESP32BTsensor" || deviceName == "ESP32BTtest" || deviceName == "ESP32-BT-Slave") {
            _scannedDevices.update { devices ->
                val newDevice = device.toBluetoothDeviceDomain()
                if (newDevice in devices) devices else devices + newDevice
            }
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver{ isConnected, bluetoothDevice ->
        val deviceDomain = bluetoothDevice.toBluetoothDeviceDomain()

        if (isConnected) {
            // Tambahkan perangkat yang terhubung ke daftar _pairedDevices
            _pairedDevices.update { currentDevices ->
                if (deviceDomain !in currentDevices) {
                    currentDevices + deviceDomain
                } else {
                    currentDevices
                }
            }
        } else {
            // Hapus perangkat yang terputus dari daftar _pairedDevices
            _pairedDevices.update { currentDevices ->
                currentDevices.filter { it.address != deviceDomain.address }
            }
        }
        _isConnected.update { isConnected }
    }

    private var currenServerSocket: BluetoothServerSocket?= null
    private var currenClientSocket: BluetoothSocket?= null

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

            }
        )
    }




    @SuppressLint("MissingPermission")
    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            Log.e("BluetoothController", "Missing BLUETOOTH_SCAN permission.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                Log.e("BluetoothController", "Bluetooth Scan permission is not granted.")
                return
            }
        } else {
            // For older Android versions
            Log.d("BluetoothController", "Starting discovery for SDK version below S.")
        }

        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        bluetoothAdapter?.takeIf { it.isEnabled }?.let {
            if (it.isDiscovering) {
                Log.d("BluetoothController", "Discovery already in progress.")
                return
            }
            it.startDiscovery()
            Log.d("BluetoothController", "Started Bluetooth discovery.")
        } ?: Log.e("BluetoothController", "Bluetooth adapter is null or not enabled.")

        // Update paired devices and try to bond with specific devices
        updatePairedDevices()

        // Collect scanned devices to check for specific devices
        CoroutineScope(Dispatchers.IO).launch {
            _scannedDevices.collect { scannedDevices ->
                val specificDevice = scannedDevices.find {
                    it.name == "ESP32BTsensor" ||
                    it.name == "ESP32BTtest" ||
                    it.name == "ESP32-BT-Slave"
                }

                if (specificDevice != null) {
                    // Stop discovery once the device is found
                    stopDiscovery()

                    // Attempt to bond with the specific device
                    val device = bluetoothAdapter?.getRemoteDevice(specificDevice.address)
                    if (device != null) {
                        try {
                            device.createBond()
                            Log.d("BluetoothController", "Bonding with specific device started.")
                        } catch (e: Exception) {
                            Log.e("BluetoothController", "Failed to bond with device: ${e.message}")
                        }
                    } else {
                        Log.e("BluetoothController", "Failed to retrieve device for bonding.")
                    }
                }
            }
        }
    }



    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            Log.e("BluetoothController", "Missing BLUETOOTH_SCAN permission.")
            return
        }

        bluetoothAdapter?.takeIf { it.isDiscovering }?.let {
            it.cancelDiscovery()
            Log.d("BluetoothController", "Stopped Bluetooth discovery.")
        } ?: Log.d("BluetoothController", "Discovery not in progress or adapter is null.")
    }


    //Dieksekusi oleh perangkat a untuk membuat server bluetooth dalam perangkatnya
    //Ini berfungsi membuat server bluetooth di android
    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)&&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                throw SecurityException("Missing BLUETOOTH_CONNECT permission")
            }

            currenServerSocket= bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                //Ini nama server bluetoothnya
                "Aplikasi_SensorService",
                //harus pakai UUID yang sama antar 2 perangkat biar bisa konek
                UUID.fromString(SERVICE_UUID)
            )

            //mengaktifkan server bluetooth dan membuat koneksi antara 2 perangkat
            var shouldLoop = true
            while(shouldLoop) {
                currenClientSocket = try {
                    currenServerSocket?.accept()
                } catch(e: IOException) {
                    shouldLoop = false
                    null
                }

                if (currenClientSocket == null) {
                    // Jika tidak ada perangkat yang terhubung, ulangi proses.
                    emit(ConnectionResult.NoDeviceConnected)
                    continue
                }

                // Jika ada perangkat yang mencoba konek
                emit(ConnectionResult.ConnectionAttempt)

                if (isDeviceReady(currenClientSocket)) {
                    emit(ConnectionResult.ConnectionEstablished)

                    currenServerSocket?.close()

                    val service = BluetoothDataTransferService(currenClientSocket!!)
                    dataTransferService = service

                    emitAll(
                        service
                            .listenForIncomingMessages()
                            .map {
                                ConnectionResult.TransferSucceded(it)
                            }
                    )
                } else {
                    // Jika perangkat tidak siap, putuskan koneksi dan ulangi
                    currenClientSocket?.close()
                    emit(ConnectionResult.DeviceNotReady)
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    // Fungsi untuk mengecek apakah perangkat benar-benar siap
    private fun isDeviceReady(socket: BluetoothSocket?): Boolean {
        // Tambahkan logika untuk mengecek status perangkat
        return try {
            // 1. Memeriksa nama perangkat
            val deviceName = socket?.remoteDevice?.name
            if (deviceName == null || deviceName != "ESP32BTsensor" ||  deviceName != "ESP32BTtest" || deviceName != "ESP32-BT-Slave") {
                // Jika nama perangkat tidak sesuai, kembalikan false
                return false
            }

            return true
        } catch (e: IOException) {
            // Jika ada kesalahan dalam komunikasi, anggap perangkat tidak siap
            false
        }
    }


    //Dieksekusi oleh perangkat b untuk konek ke server bluetooth yang sudah dibuat (alias si arduino)
    //Jangan lupa ubah buat nyari si perangkat aja
    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                throw SecurityException("Missing BLUETOOTH_CONNECT permission")
            }

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

            // Check if the device is already paired
            val isPaired = bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true
            val isSpecificDevice = device.name == "ESP32BTsensor" || device.name == "ESP32BTtest" || device.name == "ESP32-BT-Slave"

            if (!isPaired || !isSpecificDevice) {
                // Start scanning if not paired or not the specific device
                startDiscovery()

                // Wait for the device to be found during scanning
                _scannedDevices.collect { scannedDevices ->
                    val foundDevice = scannedDevices.find {
                        it.name == "ESP32BTsensor" ||
                        it.name == "ESP32BTtest" ||
                        it.name == "ESP32-BT-Slave"
                    }

                    if (foundDevice != null) {
                        // Stop scanning when the desired device is found
                        stopDiscovery()
                        bluetoothDevice?.createBond()
                        emit(ConnectionResult.ConnectionAttempt)
                        Log.d("BluetoothController", "Specific device found, pairing started.")
                    }
                }
            }

            if (isSpecificDevice) {
                // Automatically connect to the device if it's the specific one
                currenClientSocket = bluetoothDevice
                    ?.createRfcommSocketToServiceRecord(
                        UUID.fromString(SERVICE_UUID)
                    )

                stopDiscovery()

                currenClientSocket?.let { socket ->
                    try {
                        socket.connect()
                        emit(ConnectionResult.ConnectionEstablished)

                        BluetoothDataTransferService(socket).also {
                            dataTransferService = it
                            emitAll(
                                it.listenForIncomingMessages()
                                    .map { ConnectionResult.TransferSucceded(it) }
                            )
                        }
                    } catch (e: IOException) {
                        socket.close()
                        currenClientSocket = null
                        emit(ConnectionResult.Error("Connection Failed"))
                    }
                }
            } else {
                emit(ConnectionResult.Error("Device not found or not a specific device"))
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }



    //ini kalau perangkat androidnya pen kirim pesan
    override suspend fun trySendMessage(message: String): BluetoothMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)&&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            throw SecurityException("Missing BLUETOOTH_CONNECT permission")
        }

        if (dataTransferService==null){
            return null
        }

        val bluetoothMessage = BluetoothMessage (
            message = message,
            senderName = bluetoothAdapter?.name ?: "Tanpa Nama",
            isFromLocalUser = true
        )

        dataTransferService?.sendMessage(bluetoothMessage.toByteArray())

        return bluetoothMessage
    }

    //menutup koneksi antara 2 perangkat yang ke konek
    override fun closeConnection() {
        currenClientSocket?.close()
        currenServerSocket?.close()
        currenClientSocket = null
        currenServerSocket = null
    }


    override fun release() {
        try {
            context.unregisterReceiver(foundDeviceReceiver)
            context.unregisterReceiver(bluetoothStateReceiver)
            closeConnection()
            Log.d("BluetoothController", "Receiver unregistered.")
        } catch (e: IllegalArgumentException) {
            Log.e("BluetoothController", "Receiver was not registered or already unregistered.")
        }
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)&&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e("BluetoothController", "Missing BLUETOOTH_CONNECT permission.")
            return
        }

        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.filter { device ->
                // Only keep devices with the specified names
                device.name == "ESP32BTsensor" ||
                device.name == "ESP32BTtest" ||
                device.name == "ESP32-BT-Slave"
            }
            ?.also { devices ->
                _pairedDevices.update { devices }
                Log.d("BluetoothController", "Updated paired devices.")
            }
            ?: Log.e("BluetoothController", "Bluetooth adapter is null.")
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    companion object{
        const val SERVICE_UUID ="00001101-0000-1000-8000-00805F9B34FB"
    }
}
