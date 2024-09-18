import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.navigation.NavHostController
import com.example.bluetoothdevice.domain.chat.BluetoothMessage
import com.example.bluetoothdevice.presentation.BluetoothUIState
import com.example.bluetoothdevice.presentation.Screen
import com.example.bluetoothdevice.presentation.TextToSpeechViewModel
import kotlinx.coroutines.delay
import java.util.Locale
@Composable
fun DisplayTextAudioAndVibrationScreen(
    stateBluetooth:BluetoothUIState,
    messages: List<BluetoothMessage>,
    textToSpeechViewModel: TextToSpeechViewModel,
    navHostController: NavHostController
) {
    Log.d("DisplayTextAudioAndVibrationScreen", "Di Screen Display")
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    val ttsState = textToSpeechViewModel.state.value

    DisposableEffect(context) {
        // Initialization or setup code, if needed

        onDispose {
            // Cleanup code
            Log.d("DisplayTextAudioAndVibrationScreen", "Cleaning up resources")
            // Example: cancel ongoing coroutines or release resources
            // textToSpeechViewModel.cancelOngoingTasks() // Example method
        }
    }


    if(stateBluetooth.isConnected){
        VibrationAndDisplayAudio(
            modifier = Modifier,
            context = context,
            messages,
            textToSpeechViewModel,
            vibrator
        )
    }else{
        navHostController.navigate(Screen.DeviceScreen.route)
    }


}

@Composable
fun VibrationAndDisplayAudio(
    modifier: Modifier,
    context: Context,
    messages: List<BluetoothMessage>,
    textToSpeechViewModel: TextToSpeechViewModel,
    vibrator:Vibrator
){
    var lastVibrationTime by remember { mutableStateOf(0L) }
    val currentTime = System.currentTimeMillis()

    var showInvalidMessage by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        items(messages) { message ->
            // Split the message into a String array
            val stringArray = message.message.split("/").toTypedArray()

            // Check if the array has the expected size and convert to respective types
            if (stringArray.size == 4) {
                val durasiGetaran = stringArray[0].toLongOrNull() ?: 0L
                val amplitudoGetaran = stringArray[1].toIntOrNull() ?: 0
                val durasiIntervalCooldown = stringArray[2].toLongOrNull() ?: 0L
                val jarak = stringArray[3].toFloatOrNull() ?: 0f

                // Vibrate and speak when the message is received
                LaunchedEffect(message) {
                    if (currentTime - lastVibrationTime >= durasiIntervalCooldown) {
                        lastVibrationTime = currentTime
                        Log.d("DisplayTextAudioAndVibrationScreen", "Vibrating for $durasiGetaran milliseconds with amplitude $amplitudoGetaran.")

                        // Vibrate for Android 8 (Oreo) and above
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val vibrationEffect = VibrationEffect.createOneShot(durasiGetaran, amplitudoGetaran)
                            vibrator.vibrate(vibrationEffect)
                        } else {
                            // Vibrate for older versions of Android
                            vibrator.vibrate(durasiGetaran)
                        }
                    }

                    // Speak the message using TextToSpeech
                    textToSpeechViewModel.onTextFieldValueChange("Jaraknya adalah $jarak centimeter")
                    textToSpeechViewModel.textToSpeech(context)

                    Log.d("DisplayTextAudioAndVibrationScreen", "Speaking: Jaraknya adalah $jarak centimeter")
                }

                // Display text including the distance information with delay
                LaunchedEffect(jarak) {
                    delay(1000) // Delay of 1 second before displaying
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (message.isFromLocalUser) Color(0xFFD0BCFF) else Color.LightGray)
                        .padding(10.dp)
                        .widthIn(max = 250.dp, min = 60.dp)
                ) {
                    Text(
                        text = message.senderName,
                        fontSize = 10.sp,
                        color = Color.Red
                    )
                    Text(
                        text = "Jarak: $jarak centimeter",
                        fontSize = 10.sp,
                        color = Color.Black
                    )
                }
            } else if (showInvalidMessage) {
                // Handle invalid message and show the error only once
                LaunchedEffect(message) {
                    // Show the invalid message
                    textToSpeechViewModel.onTextFieldValueChange("error")
                    textToSpeechViewModel.textToSpeech(context)
                    Log.e("DisplayTextAudioAndVibrationScreen", "Received invalid message: ${message.message}")

                    // Update the flag to prevent further error messages
                    showInvalidMessage = false
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Red)
                        .padding(10.dp)
                ) {
                    Text(text = "Pesan tidak valid", color = Color.White)
                }
            }
        }
    }
}

