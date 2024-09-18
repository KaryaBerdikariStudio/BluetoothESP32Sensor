package com.example.bluetoothdevice.domain.chat

import java.io.IOException

class TransferFailedServiceException:IOException("Reading incoming data failed") {

}