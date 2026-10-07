package com.victor.relojdigitalsegundos

// Importaciones nativas del Sistema Android
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager

// Importaciones de Actividades y la nueva Extensión del Gesto de Atrás
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent

// Importaciones de la interfaz de Jetpack Compose
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

// Importaciones de Corrutinas y Utilidades de Tiempo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

// Formateador de fecha configurado para el formato de 24 horas con segundos, alojado en memoria global estática (se crea una sola vez).
private val clockFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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

        // Interceptamos el cierre de Jetpack Compose cuando el usuario desliza por completo la pantalla para salir.
        onBackPressedDispatcher.addCallback(this) {
            // 1. Apagamos el segundero
            isClockRunning = false
            
            // 2. Destruimos fulminantemente el servicio para borrar el icono/punto de la pantalla
            stopService(Intent(this@MainActivity, MainService::class.java))
            
            // 3. Forzamos al sistema operativo a cerrar y limpiar la app de raíz
            finishAndRemoveTask()
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
    
    override fun onDestroy() {
        // Comprobamos si la actividad se está cerrando definitivamente por acción del usuario, y en ese caso detenemos el servicio.
        if (isFinishing) {
            stopService(Intent(this, MainService::class.java))
        }
        super.onDestroy()
    }
}

@Composable
fun ClockSecondsApp(
    isClockRunning: Boolean, 
    initialKeepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit
) {
    // Variable de estado para almacenar la hora formateada actual (Optimizada con máscara fija inicial)
    var currentTime by remember { mutableStateOf("--:--:--") }
    
    // Estado local para rastrear si la pantalla debe permanecer encendida
    var isKeepScreenOnEnabled by remember(initialKeepScreenOn) { mutableStateOf(initialKeepScreenOn) }

    // Evita que Compose tenga que calcular la estructura condicional de color cada vez que cambia el String de los segundos.
    val textColor by remember {
        derivedStateOf {
            if (isKeepScreenOnEnabled) Color(0xFF880000) else Color.White
        }
    }

    // El bucle del segundero se sincroniza estrictamente con el estado de la pantalla (isClockRunning)
    LaunchedEffect(isClockRunning) {
        if (isClockRunning) {
            while (isActive) {
                // Leemos el tiempo del procesador en milisegundos directamente, evitando instanciar y destruir el pesado objeto Calendar() 60 veces por minuto en la memoria RAM.
                val currentTimeMillis = System.currentTimeMillis()
                currentTime = clockFormatter.format(currentTimeMillis)
                
                // Sincronización inteligente basándonos en los milisegundos de hardware del procesador
                val milisegundosFaltantes = 1000 - (currentTimeMillis % 1000)
                delay(milisegundosFaltantes)
            }
        }
    }

    // Contenedor principal que ocupa todo el espacio y proporciona restricciones de tamaño
    BoxWithConstraints(
        // Modificadores encadenados estáticos para evitar la reinstanciación en el redibujado de Compose
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
        
        // Registra las matemáticas del tamaño del texto de manera estable. No se recalculará cada un segundo.
        val currentDensity = LocalDensity.current
        val calculatedFontSize = remember(availableWidthDp, currentDensity) {
            val widthInPx = with(currentDensity) { availableWidthDp.toPx() }
            // Factor empírico para ajustar 8 caracteres ("HH:mm:ss") de forma óptima
            val idealSizeSp = (widthInPx / 8.0f) * 1.55f / currentDensity.density
            // Limita el tamaño máximo para evitar desbordamientos en pantallas muy grandes
            idealSizeSp.coerceAtMost(64f).sp
        }

        // Muestra el texto de la hora con el estilo y tamaño calculados
        Text(
            text = currentTime,
            // Cambia el color basándose en el estado derivado optimizado
            color = textColor,
            // Asegura que todo el texto permanezca estrictamente en una sola línea
            maxLines = 1,
            style = androidx.wear.compose.material.MaterialTheme.typography.display2.copy(
                fontSize = calculatedFontSize,
                // OPTIMIZACIÓN EXTREMA DE FUENTE: Desactiva cálculos de padding tipográfico dinámico.
                // Le dice al procesador de fuentes de Android que renderice los números de forma directa y fija.
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
