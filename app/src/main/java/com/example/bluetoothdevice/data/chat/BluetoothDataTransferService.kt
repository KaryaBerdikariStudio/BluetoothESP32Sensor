package com.example.bluetoothdevice.data.chat

import android.bluetooth.BluetoothSocket
import com.example.bluetoothdevice.domain.chat.BluetoothMessage
import com.example.bluetoothdevice.domain.chat.ConnectionResult
import com.example.bluetoothdevice.domain.chat.TransferFailedServiceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException


class BluetoothDataTransferService (
    private val socket: BluetoothSocket
) {

    //Disini tuh datanya  nanti di ubah balik jadi string
    fun listenForIncomingMessages():Flow<BluetoothMessage>{
        return flow{
            if(!socket.isConnected){
                return@flow
            }
            val buffer = ByteArray(1024)
            while (true){
                val byteCount = try{
                    socket.inputStream.read(buffer)
                }catch (e:IOException){
                    throw  TransferFailedServiceException()
                }

                //ini nerima data dari perangkat b
                emit(
                    buffer.decodeToString(
                        endIndex = byteCount
                    ).toBluetoothMessage(
                        isFromLocalUser = false
                    )
                )

            }
        }.flowOn(Dispatchers.IO)
    }

    //Disini fungsi ngirim pesan
    suspend fun sendMessage(bytes: ByteArray):Boolean{
        return withContext(Dispatchers.IO){
            try{
                socket.outputStream.write(bytes)
            }catch(e:IOException){
                e.printStackTrace()
                return@withContext false
            }

            true
        }
    }

}