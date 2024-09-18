package com.example.bluetoothdevice.presentation

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluetoothdevice.ui.theme.BluetoothDeviceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothDeviceTheme {
                val  bluetoothViewModel = hiltViewModel<BluetoothViewModel>()
                val bluetoothState by bluetoothViewModel.state.collectAsState()

                val ttsViewModel:TextToSpeechViewModel = viewModel()

                Navigation(
                    bluetoothState,
                    bluetoothViewModel,
                    onBluetoothStateChanged = {
                        showBluetoothDialog()
                    },
                    bluetoothAdapter,
                    ttsViewModel,
                    this
                )

            }
        }
    }

    //Memaksa Nyalakan Bluetooth
    private var isBluetootDialogAlreadyShown = false
    private fun showBluetoothDialog(){
        if(!bluetoothAdapter.isEnabled){
            if(!isBluetootDialogAlreadyShown){
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startBluetoothIntentForResult.launch(enableBluetoothIntent)
                isBluetootDialogAlreadyShown = true
            }
        }
    }

    private val startBluetoothIntentForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            isBluetootDialogAlreadyShown = false
            if(result.resultCode != Activity.RESULT_OK){
                showBluetoothDialog()
            }
        }


}






