package com.victor.relojdigitalsegundos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var isAppVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockSecondsApp(isAppVisible = isAppVisible)
        }
    }

    override fun onResume() {
        super.onResume()
        isAppVisible = true
    }

    override fun onPause() {
        super.onPause()
        isAppVisible = false // Detiene la corrutina de los segundos inmediatamente para consumo CERO

        // Si el usuario desliza para volver atrás o pulsa el botón físico de Home, 
        // Wear OS marcará la actividad como 'isFinishing'.
        // Si la pantalla solo se apaga por desuso, 'isFinishing' será FALSE.
        if (isFinishing) {
            finish() 
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Forzar la muerte del proceso solo si la app realmente se está cerrando de forma definitiva
        if (isFinishing) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

@Composable
fun ClockSecondsApp(isAppVisible: Boolean) {
    var currentTime by remember { mutableStateOf("--:--:--") }
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(isAppVisible) {
        if (isAppVisible) {
            while (isActive) {
                currentTime = formatter.format(Date())
                delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentTime,
            color = Color.White,
            style = androidx.wear.compose.material.MaterialTheme.typography.display2
        )
    }
}
