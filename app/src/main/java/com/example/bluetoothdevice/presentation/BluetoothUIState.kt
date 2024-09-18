package com.example.bluetoothdevice.presentation

import com.example.bluetoothdevice.domain.chat.BluetoothDeviceList
import com.example.bluetoothdevice.domain.chat.BluetoothMessage

data class BluetoothUIState(
    val scannedDevices : List<BluetoothDeviceList> = emptyList(),
    val pairedDevices: List<BluetoothDeviceList> = emptyList(),
    val bluetoothDevices: List<BluetoothDeviceList> = emptyList(),
    val isConnected:Boolean = false,
    val isConnecting:Boolean = false,
    val errorMessage:String? = null,
    val messages: List<BluetoothMessage> = emptyList()
)
