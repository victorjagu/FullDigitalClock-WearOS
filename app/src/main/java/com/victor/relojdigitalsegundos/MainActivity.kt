package com.victor.relojdigitalsegundos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    // Estado reactivo que avisa a Compose si el segundero debe estar activo
    private var isClockRunning by mutableStateOf(false)
    private var isKeepScreenOnEnabledRaw = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Nos aseguramos de limpiar la bandera al iniciar para evitar que se quede pegada
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Solicitamos permiso de notificaciones en Android 13+ (Wear OS 4+) antes de lanzar el servicio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        // Arrancamos tu nuevo MainService.
        // Esto le avisa a Wear OS que la app tiene una tarea en curso y no debe cerrarse en segundo plano.
        val intentService = Intent(this, MainService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentService)
        } else {
            startService(intentService)
        }

        setContent {
            ClockSecondsApp(
                isClockRunning = isClockRunning,
                initialKeepScreenOn = isKeepScreenOnEnabledRaw,
                onKeepScreenOnChanged = { keepOn ->
                    isKeepScreenOnEnabledRaw = keepOn
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

    // CONTROL DEL CICLO DE VIDA PARA TRABAJAR EN NEGRO ABSOLUTO:
    override fun onStart() {
        super.onStart()
        // La pantalla se enciende (o entramos a la app): Se reactiva el segundero inmediatamente
        isClockRunning = true 
        isKeepScreenOnEnabledRaw = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        // La pantalla se apaga POR COMPLETO: Congelamos el segundero para consumo CERO de batería.
        // Gracias al servicio, la app se queda congelada en memoria en lugar de ser destruida por el sistema.
        isClockRunning = false 
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /*
    // DETECTA CUANDO EL USUARIO SALE VOLUNTARIAMENTE (Botón Home o Recientes)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // Verificamos si la pantalla física del reloj sigue encendida/interactiva
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        val isScreenOn = powerManager.isInteractive

        if (isScreenOn) {
            // SI LA PANTALLA SIGUE ENCENDIDA: Significa que el usuario pulsó Home voluntariamente.
            
            // 1. Detenemos el segundero
            isClockRunning = false
            
            // 2. Destruimos el servicio en primer plano para limpiar la notificación del sistema
            stopService(Intent(this, MainService::class.java))
            
            // 3. Cerramos y matamos la actividad por completo de la memoria RAM
            finish()
        }
        // Si isScreenOn es false, significa que la pantalla se apagó sola por inactividad.
        // Ignoramos el cierre y dejamos que onStop() congele la app de forma segura mediante MainService.
    }
    */
    
    override fun onDestroy() {
        super.onDestroy()
        // Si el usuario cierra la app deslizando voluntariamente hacia atrás para salir,
        // detenemos el servicio en primer plano para limpiar la notificación del sistema.
        stopService(Intent(this, MainService::class.java))
    }
}

@Composable
fun ClockSecondsApp(
    isClockRunning: Boolean, 
    initialKeepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit
) {
    // Variable de estado para almacenar la hora formateada actual
    var currentTime by remember { mutableStateOf("--:--:--") }
    // Formateador de fecha configurado para el formato de 24 horas con segundos
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    // Estado local para rastrear si la pantalla debe permanecer encendida
    var isKeepScreenOnEnabled by remember(initialKeepScreenOn) { mutableStateOf(initialKeepScreenOn) }

    // El bucle del segundero se sincroniza estrictamente con el estado de la pantalla (isClockRunning)
    LaunchedEffect(isClockRunning) {
        if (isClockRunning) {
            while (isActive) {
                // Obtiene la instancia actual del calendario con la hora del sistema
                val now = Calendar.getInstance()
                // Formatea y actualiza el estado de la hora
                currentTime = formatter.format(now.time)
                
                // Sincronización inteligente: Calculamos exactamente cuántos milisegundos
                // quedan para el siguiente segundo. Evita retrasos y desfases gráficos.
                val milisegundosFaltantes = 1000 - now.get(Calendar.MILLISECOND)
                delay(milisegundosFaltantes.toLong())
            }
        }
    }

    // Contenedor principal que ocupa todo el espacio y proporciona restricciones de tamaño
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Configura un fondo negro sólido para optimizar el consumo en pantallas OLED
            .background(Color.Black) 
            // Configura el detector de gestos táctiles en el área de la pantalla
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Alterna el estado de mantener la pantalla encendida
                        val newState = !isKeepScreenOnEnabled
                        isKeepScreenOnEnabled = newState
                        onKeepScreenOnChanged(newState)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Calcula el ancho disponible restando un margen de seguridad de 24dp
        val availableWidthDp = maxWidth - 24.dp
        
        // Calcula dinámicamente el tamaño de la fuente basado en la densidad y el ancho disponible
        val calculatedFontSize = with(LocalDensity.current) {
            val widthInPx = availableWidthDp.toPx()
            // Factor empírico para ajustar 8 caracteres ("HH:mm:ss") de forma óptima
            val idealSizeSp = (widthInPx / 8.0f) * 1.55f / density
            // Limita el tamaño máximo para evitar desbordamientos en pantallas muy grandes
            idealSizeSp.coerceAtMost(64f).sp
        }

        // Muestra el texto de la hora con el estilo y tamaño calculados
        Text(
            text = currentTime,
            // Cambia el color a rojo oscuro si está activado mantener encendido, de lo contrario blanco puro
            color = if (isKeepScreenOnEnabled) Color(0xFF880000) else Color.White,
            // Asegura que todo el texto permanezca estrictamente en una sola línea
            maxLines = 1,
            style = androidx.wear.compose.material.MaterialTheme.typography.display2.copy(
                fontSize = calculatedFontSize
            )
        )
    }
}
