package com.example.bluetoothdevice.presentation

import DeviceScreen
import DisplayTextAudioAndVibrationScreen
import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothdevice.presentation.components.StartScreen

@Composable
fun Navigation(
    state:BluetoothUIState,
    viewModelBluetooth:BluetoothViewModel,
    onBluetoothStateChanged:()->Unit,
    bluetoothAdapter: BluetoothAdapter,
    viewModelTTS:TextToSpeechViewModel,
//    testString: String,
    context: Context
){
    //Navigasi buat user kemana mana
    val navController = rememberNavController()
    val message = rememberSaveable {
        mutableStateOf("")
    }



    NavHost(navController = navController, startDestination = Screen.StartScreen.route){
        composable(Screen.StartScreen.route){
            StartScreen(
                navController = navController,
                onBluetoothStateChanged,
                bluetoothAdapter
            )
        }
        composable(Screen.DeviceScreen.route){
            DeviceScreen(
                state,
                context,
                onStartScan = viewModelBluetooth::startScan,
                onStopScan = viewModelBluetooth::stopScan,
                onDeviceClick = viewModelBluetooth::connectToDevice,
                //onStartServer = viewModelBluetooth::waitForIncomingConnection,
                viewModelTTS,
                navController
            )
        }
        composable(Screen.DisplayTextAudioAndVibrationScreen.route){
            DisplayTextAudioAndVibrationScreen(
                state,
                messages = state.messages,
                textToSpeechViewModel = viewModelTTS,
                navController
            )
        }

    }
}


sealed class Screen (val route:String){
    object StartScreen:Screen("start_screen")
    object DeviceScreen:Screen("device_screen")
    object DisplayTextAudioAndVibrationScreen:Screen("displaytextaudioandvibration_screen")
}