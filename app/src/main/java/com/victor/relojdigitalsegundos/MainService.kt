package com.victor.relojdigitalsegundos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity

class MainService : Service() {
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "FullDigitalClockChannel"

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()

        // Intent para que al tocar la notificación o el icono de actividad en curso, el reloj regrese a tu MainActivity
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Notificación en segundo plano (Requisito obligatorio de Android)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) 
            .setContentTitle("Full Digital Clock")
            .setContentText("") 
            .setOngoing(true)

        // API de Actividad en Curso (Ongoing Activity): Vincula el servicio al sistema Wear OS
        val ongoingActivity = OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
            .setTouchIntent(pendingIntent)
            .build()

        ongoingActivity.apply(applicationContext)

        // Iniciamos el servicio en primer plano sin usar WakeLocks para que la CPU pueda dormir en negro
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Clock Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    // Este método se dispara automáticamente cuando se cierra desde el menú de aplicaciones recientes.
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        // 1. Detener el modo foreground y quitar la notificación inmediatamente de la pantalla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        // 2. Detener el servicio por completo para eliminar el proceso
        stopSelf()
    }
    
}
