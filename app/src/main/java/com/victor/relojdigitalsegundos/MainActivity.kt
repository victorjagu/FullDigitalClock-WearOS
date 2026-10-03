package com.victor.relojdigitalsegundos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
    private var isScreenOff = false

    // Detecta si la pantalla física del reloj se apaga por tiempo de espera
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                isScreenOff = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Registro del receptor seguro para Wear OS 4 / Android 14+
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        setContent {
            ClockSecondsApp(isAppVisible = isAppVisible)
        }
    }

    override fun onResume() {
        super.onResume()
        isAppVisible = true
        isScreenOff = false // Resetea el estado al volver a encender la pantalla
    }

    override fun onPause() {
        super.onPause()
        isAppVisible = false // Detiene la corrutina de los segundos inmediatamente para consumo CERO de CPU

        // Si la pantalla sigue encendida, significa que el usuario pulsó Home o deslizó atrás.
        // En este escenario cerramos la app inmediatamente.
        if (!isScreenOff) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ya desregistrado
        }
        
        // Destrucción total y absoluta del proceso de la app al finalizar para liberar la memoria RAM
        if (isFinishing) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

@Composable
fun ClockSecondsApp(isAppVisible: Boolean) {
    var currentTime by remember { mutableStateOf("--:--:--") }
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // El temporizador vive estrictamente bajo el ciclo de vida visible de la interfaz
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
            .background(Color.Black), // Fondo OLED puro para apagar físicamente los píxeles
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentTime,
            color = Color.White,
            style = androidx.wear.compose.material.MaterialTheme.typography.display2
        )
    }
}
