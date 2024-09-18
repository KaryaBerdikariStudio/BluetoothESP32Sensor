package com.example.bluetoothdevice.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDeviceList>>
    val pairedDevices: StateFlow<List<BluetoothDeviceList>>

    val errors: SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    //Dieksekusi oleh perangkat android
    fun startBluetoothServer(): Flow<ConnectionResult>
    //Dieksekusi oleh perangkat arduino
    fun connectToDevice(device:BluetoothDeviceList): Flow<ConnectionResult>

    suspend fun trySendMessage(message: String):BluetoothMessage?


    fun closeConnection()
    fun release()
}