import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bluetoothdevice.domain.chat.BluetoothDeviceList
import com.example.bluetoothdevice.presentation.BluetoothUIState
import com.example.bluetoothdevice.presentation.Screen
import com.example.bluetoothdevice.presentation.TextToSpeechState
import com.example.bluetoothdevice.presentation.TextToSpeechViewModel
import kotlinx.coroutines.delay


@Composable
fun DeviceScreen(
    stateBluetooth: BluetoothUIState,
    context: Context,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDeviceList) -> Unit,
    ttsViewModel: TextToSpeechViewModel,
    navController: NavController,
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDevice by remember { mutableStateOf<BluetoothDeviceList?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var hasSpokenConnecting by remember { mutableStateOf(false) }
    var pairedDevicesChecked by remember { mutableStateOf(false) }

    Log.d("DeviceScreen", "Di Device Screen")

    // Check if paired devices meet the criteria
    LaunchedEffect(stateBluetooth.pairedDevices) {
        val devices = stateBluetooth.pairedDevices

        if (!pairedDevicesChecked) {
            selectedDevice = devices.firstOrNull {
                it.name == "ESP32BTsensor" || it.name == "ESP32BTtest" || it.name == "ESP32-BT-Slave"
            }

            if (selectedDevice != null && !isScanning) {
                isConnecting = true
                onDeviceClick(selectedDevice!!)
            } else if(selectedDevice != null) {
                onDeviceClick(selectedDevice!!)
            }else{
                isScanning = true
            }

            pairedDevicesChecked = true
        }
    }

    // Manage scanning and connection flow
    LaunchedEffect(isScanning) {
        if (isScanning) {
            onStartScan()
            delay(5000L) // 5 seconds
            onStopScan()
            isScanning = false
        }
    }

    // Show scanning UI initially if needed
    if (isScanning) {
        ScanAndPairedDevice(
            context = context,
            onStartScan = onStartScan,
            onStopScan = onStopScan,
            ttsViewModel = ttsViewModel,
            ttsState = ttsViewModel.state.value
        )
    } else if (isConnecting) {
        // Show progress when connecting
        ConnectingLoaderComponent(
            context = context,
            stateBluetooth = stateBluetooth,
            ttsViewModel = ttsViewModel,
            ttsState = ttsViewModel.state.value
        )
    }

    // Handle connection status and text-to-speech
    LaunchedEffect(stateBluetooth.isConnected) {
        hasSpokenConnecting = false
        if (stateBluetooth.isConnected && !hasSpokenConnecting) {
            Toast.makeText(context, "Anda Terhubung", Toast.LENGTH_LONG).show()
            ttsViewModel.onTextFieldValueChange("Anda Terhubung")
            ttsViewModel.textToSpeech(context)
            navController.navigate(Screen.DisplayTextAudioAndVibrationScreen.route)
            hasSpokenConnecting = true
        } else if (stateBluetooth.errorMessage != null) {
            errorMessage = stateBluetooth.errorMessage
        }
    }

    // Display error message if any
    errorMessage?.let { message ->
        Text(text = message)
    }

}



@Composable
fun ConnectingLoaderComponent(
    context: Context,
    stateBluetooth: BluetoothUIState,
    ttsViewModel: TextToSpeechViewModel,
    ttsState : TextToSpeechState
){
    var hasSpokenConnecting by remember { mutableStateOf(false) }
    var hasSpokenConnectingFailed by remember { mutableStateOf(false) }
    // Show progress when connecting
    if (stateBluetooth.isConnecting) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .height(500.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Log.d("DeviceScreen", "State Connecting")

            CircularProgressIndicator()
            Spacer(modifier = Modifier.width(16.dp))

            Text(text = "Menghubungkan...")

            // If TTS is enabled, read the "Menghubungkan" message
            if (ttsState.isTTSEnabled && !hasSpokenConnectingFailed) {
                Log.d("DeviceScreen", "TTS Enabled")

                ttsViewModel.onTextFieldValueChange("Menghubungkan")
                ttsViewModel.textToSpeech(context)
                hasSpokenConnectingFailed  = true
            } else {
                Log.d("DeviceScreen", "TTS Disabled")
                ttsViewModel.enabledTTS()
            }


        }
    }else if (stateBluetooth.errorMessage == "Connection Failed"){
        Log.d("DeviceScreen", "Failed to Connect")



        Column (
            modifier = Modifier
                .fillMaxSize()
                .height(500.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.width(16.dp))

            Text(text = "Gagal Menghubungkan ke Perangkat")
        }
        // If TTS is enabled, read the "Menghubungkan" message
        if (ttsState.isTTSEnabled && !hasSpokenConnectingFailed) {
            Log.d("DeviceScreen", "TTS Enabled")

            ttsViewModel.onTextFieldValueChange("Gagal Mengubungkan ke Perangkat")
            ttsViewModel.textToSpeech(context)
            hasSpokenConnectingFailed = true
        } else {
            Log.d("DeviceScreen", "TTS Disabled")
            ttsViewModel.enabledTTS()
        }
    }
}


@Composable
fun ScanAndPairedDevice (
    context: Context,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    ttsViewModel: TextToSpeechViewModel,
    ttsState: TextToSpeechState
){
    var hasSpokenConnecting by remember { mutableStateOf(false) }
    var connectionAttempt by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ){
        Column (
            modifier = Modifier
                .fillMaxSize()
                .height(500.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Log.d("DeviceScreen", "State Connecting")


            Spacer(modifier = Modifier.width(16.dp))

            Text(text = "Mencari Perangkat")
            if (ttsState.isTTSEnabled && !hasSpokenConnecting) {
                Log.d("DeviceScreen", "TTS Enabled")

                ttsViewModel.onTextFieldValueChange("Mencari Perangkat")
                ttsViewModel.textToSpeech(context)
                hasSpokenConnecting = true
            } else {
                Log.d("DeviceScreen", "TTS Disabled")
                ttsViewModel.enabledTTS()
            }
        }


        // Scan logic with 5-second delay
        LaunchedEffect(connectionAttempt) {
            if (connectionAttempt < 5) {
                isScanning = true
                onStartScan()
                delay(5000L) // 5 seconds
                onStopScan()
                connectionAttempt += 1
                isScanning = false
            }
        }


    }
}


