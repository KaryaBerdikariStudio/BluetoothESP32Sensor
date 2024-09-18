package com.example.bluetoothdevice.data.chat

import com.example.bluetoothdevice.domain.chat.BluetoothMessage

//Disini nanti data pesannya dari siapa dan apa diubah jadi byte array
fun String.toBluetoothMessage(isFromLocalUser:Boolean):BluetoothMessage{
    val name = substringBeforeLast("#")
    val message = substringAfter("#")

    return BluetoothMessage(
        message = message,
        senderName = name,
        isFromLocalUser = isFromLocalUser
    )
}

fun BluetoothMessage.toByteArray(): ByteArray {
    return "$senderName#$message".encodeToByteArray()
}