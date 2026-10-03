package com.victor.relojdigitalsegundos

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var isAppVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 💡 ASEGURA STARTUP EN FALSE: Forzamos a que la ventana limpie la bandera al abrir la app.
        // Así el reloj se iniciará siempre respetando el tiempo de apagado normal.
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            // Pasamos la función para controlar la pantalla desde Compose
            ClockSecondsApp(
                isAppVisible = isAppVisible,
                onKeepScreenOnChanged = { keepOn ->
                    toggleKeepScreenOn(keepOn)
                }
            )
        }
    }

    // Activa o desactiva de forma dinámica la bandera para mantener la pantalla encendida
    private fun toggleKeepScreenOn(keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onResume() {
        super.onResume()
        isAppVisible = true
    }

    override fun onPause() {
        super.onPause()
        isAppVisible = false // Detiene la corrutina de los segundos inmediatamente para consumo CERO de CPU

        // Seguridad: Se limpia la bandera al salir para asegurar el comportamiento normal del reloj
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Si el usuario sale adrede (desliza atrás o botón Home), cerramos la app inmediatamente.
        if (isFinishing) {
            finish() 
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destrucción total y absoluta del proceso de la app al finalizar para liberar la memoria RAM
        if (isFinishing) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

@Composable
fun ClockSecondsApp(
    isAppVisible: Boolean, 
    onKeepScreenOnChanged: (Boolean) -> Unit
) {
    var currentTime by remember { mutableStateOf("--:--:--") }
    
    // Estado que recuerda si la pantalla debe quedarse encendida o no (inicia estrictamente en false)
    var isKeepScreenOnEnabled by remember { mutableStateOf(false) }
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
            .background(Color.Black) // Fondo OLED puro para apagar físicamente los píxeles
            // Captura los toques en cualquier píxel de la pantalla (fondo o texto) sin añadir efectos visuales pesados
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isKeepScreenOnEnabled = !isKeepScreenOnEnabled
                        onKeepScreenOnChanged(isKeepScreenOnEnabled)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentTime,
            // TEXTO ROJO TENUE: Apaga por completo los subpíxeles verde y azul del panel OLED.
            // Al usar un tono apagado (0xFF880000) en vez de rojo brillante, reducimos el consumo eléctrico drásticamente.
            color = if (isKeepScreenOnEnabled) Color(0xFF880000) else Color.White,
            style = androidx.wear.compose.material.MaterialTheme.typography.display2
        )
    }
}
