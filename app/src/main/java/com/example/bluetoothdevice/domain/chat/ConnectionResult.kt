package com.example.bluetoothdevice.domain.chat

sealed interface ConnectionResult {
    object ConnectionEstablished:ConnectionResult
    object DeviceNotReady : ConnectionResult
    object NoDeviceConnected : ConnectionResult
    object ConnectionAttempt : ConnectionResult

    data class TransferSucceded(val message:BluetoothMessage):ConnectionResult

    data class Error(val message:String):ConnectionResult
}