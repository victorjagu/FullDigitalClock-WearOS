package com.victor.relojdigitalsegundos

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var isAppVisible by mutableStateOf(false)
    
    // Estado interno que controla si la pantalla debe quedarse encendida o no (inicia estrictamente en false)
    private var isKeepScreenOnEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ASEGURA STARTUP EN FALSE: Forzamos a que la ventana limpie la bandera al abrir la app.
        // Así el reloj se iniciará siempre respetando el tiempo de apagado normal.
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            // Pasamos la función para controlar la pantalla desde Compose
            ClockSecondsApp(
                isAppVisible = isAppVisible,
                isKeepScreenOnEnabled = isKeepScreenOnEnabled,
                onKeepScreenOnChanged = { keepOn ->
                    isKeepScreenOnEnabled = keepOn
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

    // SOLUCIÓN AL GESTO DE DESLIZAR: Al volver a entrar a la app tras haber deslizado, 
    // se ejecuta onStart. Forzamos a limpiar cualquier estado residual previo.
    override fun onStart() {
        super.onStart()
        isKeepScreenOnEnabled = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
    isKeepScreenOnEnabled: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit
) {
    var currentTime by remember { mutableStateOf("--:--:--") }
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    // 💡 Tamaño máximo deseado para relojes grandes (se adaptará reduciéndose si es necesario)
    var fontSize by remember { mutableStateOf(64.sp) }

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
                        onKeepScreenOnChanged(!isKeepScreenOnEnabled)
                    }
                )
            }
            .padding(horizontal = 12.dp), // Margen lateral para proteger el texto en esferas circulares
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentTime,
            color = if (isKeepScreenOnEnabled) Color(0xFF880000) else Color.White,
            maxLines = 1, // 💡 Prohíbe terminantemente el salto de línea
            overflow = TextOverflow.Clip, // Corta visualmente el sobrante en el milisegundo exacto antes de encogerse
            onTextLayout = { textLayoutResult ->
                // 💡 Si detecta que no cabe a lo ancho, reduce el tamaño de la letra inmediatamente
                if (textLayoutResult.hasVisualOverflow) {
                    fontSize = (fontSize.value - 1).sp
                }
            },
            style = androidx.wear.compose.material.MaterialTheme.typography.display2.copy(
                fontSize = fontSize
            )
        )
    }
}
