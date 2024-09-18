package com.example.bluetoothdevice.presentation.components

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bluetoothdevice.permissions.PermissionUtils
import com.example.bluetoothdevice.presentation.Screen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartScreen(
    navController: NavController,
    onBluetoothStateChanged:()->Unit,
    bluetoothAdapter: BluetoothAdapter
) {

    //Nih Screen buat permission ama Bluetooth doang nanti dijadiin Splash Screen aja,
    //Kalau animasi dah habis baru lu perm check ama pastiin bluetooth nyala (looping)


    //WOI INGAT BIKIN SPLASH SCREEN


    val permissionState = rememberMultiplePermissionsState(permissions = PermissionUtils.permissions)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.Blue, CircleShape)
                .clickable {
                    if (permissionState.allPermissionsGranted){
                        if (bluetoothAdapter.isEnabled){
                            navController.navigate(Screen.DeviceScreen.route) {
                                popUpTo(Screen.DeviceScreen.route) {
                                    inclusive = true
                                }
                            }
                        }else{
                            onBluetoothStateChanged()
                        }
                    }else{
                        permissionState.launchMultiplePermissionRequest()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Mulai",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


