package com.example.bluetoothdevice.presentation

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

import androidx.lifecycle.ViewModel
import java.util.Locale

class TextToSpeechViewModel:ViewModel() {
    private val _state = mutableStateOf(TextToSpeechState())
    val state: State<TextToSpeechState> = _state

    //Initialize TTS
    private  var  textToSpeech: TextToSpeech? = null


    //disini menyimpan string buat TTS
    fun onTextFieldValueChange(text:String){
        _state.value = state.value.copy(
            text = text
        )
    }

    fun enabledTTS(){
        _state.value = state.value.copy(
            isTTSEnabled = true
        )
    }

    fun disabledTTS(){
        _state.value = state.value.copy(
            isTTSEnabled = false
        )
    }

    fun textToSpeech(context: Context){
        _state.value = state.value.copy(
            isTTSEnabled = false
        )
        textToSpeech = TextToSpeech(
            context
        ) {
            if (it == TextToSpeech.SUCCESS) {
                textToSpeech?.let { txtToSpeech ->
                    txtToSpeech.language = Locale("id", "ID")
                    txtToSpeech.setSpeechRate(1.0f)
                    txtToSpeech.speak(
                        _state.value.text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                }
            }
            _state.value = state.value.copy(
                isTTSEnabled = true
            )
        }
    }
}