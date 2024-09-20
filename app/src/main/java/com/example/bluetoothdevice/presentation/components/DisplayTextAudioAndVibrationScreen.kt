import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bluetoothdevice.domain.chat.BluetoothMessage
import com.example.bluetoothdevice.presentation.BluetoothUIState
import com.example.bluetoothdevice.presentation.Screen
import com.example.bluetoothdevice.presentation.TextToSpeechViewModel

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
    vibrator: Vibrator
) {
    var lastVibrationTime by remember { mutableStateOf(0L) }
    var showInvalidMessage by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        items(messages) { message ->
            val stringMessage = message.message

            val jarak = stringMessage.toFloat()

            // Vibrate and speak when the message is received
            LaunchedEffect(message, jarak != 0f) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastVibrationTime >= calculateCooldown(jarak / 100f)) {
                    lastVibrationTime = currentTime
                    VibratePhone(jarak, vibrator)
                }
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
        }
    }
}

fun calculateCooldown(jarakMeter: Float): Long {
    return if (jarakMeter <= 0.5f) {
        200L
    } else if (jarakMeter >= 5) {
        1000L
    } else {
        ((177.78f * jarakMeter) + 111.11f).toLong()
    }
}

fun VibratePhone(
    jarak: Float,
    vibrator: Vibrator
) {
    var jarakMeter: Float = jarak / 100f

    val durasiGetaran: Long = 100L
    val amplitudoGetaran: Int = when {
        jarakMeter <= 0.5f -> 255
        jarakMeter >= 5 -> 100
        else -> ((-34.44f * jarakMeter) + 272.22f).toInt()
    }

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



@Composable
fun TTSJarak(
    context: Context,
    jarak: Float,
    textToSpeechViewModel: TextToSpeechViewModel
){
    textToSpeechViewModel.onTextFieldValueChange("Jaraknya adalah $jarak centimeter")
    textToSpeechViewModel.textToSpeech(context)

    Log.d("DisplayTextAudioAndVibrationScreen", "Speaking: Jaraknya adalah $jarak centimeter")
}