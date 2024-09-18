package com.example.bluetoothdevice.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothdevice.domain.chat.BluetoothController
import com.example.bluetoothdevice.domain.chat.BluetoothDeviceDomain
import com.example.bluetoothdevice.domain.chat.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothUIState())
    private var deviceConnectionJob: Job? = null

    //Mengubah data State ke compose view alias UI
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        Log.d("BluetoothViewModel", "State updated: Scanned Devices: ${scannedDevices.size}, Paired Devices: ${pairedDevices.size}")
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            messages = if (state.isConnected) state.messages else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    init {
        Log.d("BluetoothViewModel", "ViewModel initialized")
        bluetoothController.isConnected.onEach { isConnected ->
            Log.d("BluetoothViewModel", "Connection status updated: $isConnected")
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            Log.d("BluetoothViewModel", "Error received: $error")
            _state.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    //Menghubungkan ke perangkat
    fun connectToDevice(device: BluetoothDeviceDomain) {
        Log.d("BluetoothViewModel", "Connecting to device: ${device.name}")
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        Log.d("BluetoothViewModel", "Disconnecting from device")
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = false
            )
        }
    }

    fun waitForIncomingConnection() {
        Log.d("BluetoothViewModel", "Waiting for incoming connection")
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .startBluetoothServer()
            .listen()
    }

    fun sendMessage(message: String) {
        Log.d("BluetoothViewModel", "Sending message: $message")
        viewModelScope.launch {
            val bluetoothMessage = bluetoothController.trySendMessage(message)
            if (bluetoothMessage != null) {
                Log.d("BluetoothViewModel", "Message sent successfully: $bluetoothMessage")
                _state.update { it.copy(messages = it.messages + bluetoothMessage) }
            }
        }
    }

    fun startScan() {
        Log.d("BluetoothViewModel", "Starting device scan")
        bluetoothController.startDiscovery()
    }

    fun stopScan() {
        Log.d("BluetoothViewModel", "Stopping device scan")
        bluetoothController.stopDiscovery()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.ConnectionEstablished -> {
                    Log.d("BluetoothViewModel", "Connection established")
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                }
                is ConnectionResult.TransferSucceded -> {
                    Log.d("BluetoothViewModel", "Message received: ${result.message}")
                    _state.update {
                        it.copy(
                            messages = it.messages + result.message
                        )
                    }
                }
                is ConnectionResult.Error -> {
                    Log.d("BluetoothViewModel", "Connection error: ${result.message}")
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                }
                ConnectionResult.DeviceNotReady -> {
                    Log.d("BluetoothViewModel", "Device not ready")
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = true,
                            errorMessage = "Device not ready"
                        )
                    }
                }
                ConnectionResult.ConnectionAttempt -> {
                    Log.d("BluetoothViewModel", "Attempting to connect")
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = true,
                            errorMessage = "Attempting to connect"
                        )
                    }
                }
                ConnectionResult.NoDeviceConnected -> {
                    Log.d("BluetoothViewModel", "No device connected")
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = true,
                            errorMessage = "No device connected"
                        )
                    }
                }
            }
        }
            .catch { throwable ->
                Log.e("BluetoothViewModel", "Error in connection flow: ${throwable.message}")
                bluetoothController.closeConnection()
                _state.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BluetoothViewModel", "ViewModel cleared")
        bluetoothController.release()
    }
}
