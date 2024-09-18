package com.example.bluetoothdevice.data.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.bluetoothdevice.domain.chat.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain{
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}