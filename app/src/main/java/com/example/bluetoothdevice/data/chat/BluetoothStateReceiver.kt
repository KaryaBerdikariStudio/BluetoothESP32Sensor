package com.example.bluetoothdevice.data.chat

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BluetoothStateReceiver(
    private val onStateChangedFound: (isConnected:Boolean, BluetoothDevice)-> Unit
): BroadcastReceiver() {
    //kalau mennerima bluetooth
    override fun onReceive(context: Context?, intent: Intent?) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            )
        } else {
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        when(intent?.action){
            BluetoothDevice.ACTION_ACL_CONNECTED->{
                onStateChangedFound(true, device?:return)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED->{
                onStateChangedFound(false, device?:return)
            }
        }
    }

}