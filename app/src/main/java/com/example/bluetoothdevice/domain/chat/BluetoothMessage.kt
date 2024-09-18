package com.example.bluetoothdevice.domain.chat


// Nih kelas buat nampung data yang dikirim
data class BluetoothMessage(
    val message: String,
    val senderName: String,
    val isFromLocalUser: Boolean
)
