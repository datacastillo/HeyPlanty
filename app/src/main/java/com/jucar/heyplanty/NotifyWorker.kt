package com.jucar.heyplanty

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotifyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // Recibimos el nombre de la planta que pasamos desde el ViewModel
        val nombrePlanta = inputData.getString("plant_name") ?: "Tu planta"

        showNotification(nombrePlanta)
        return Result.success()
    }

    private fun showNotification(nombre: String) {
        val channelId = "plant_care_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ruta de nuestro sonido personalizado
        val soundUri = Uri.parse("android.resource://${applicationContext.packageName}/${R.raw.regar_sonido}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Riego",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos para hidratar a tus plantas"
                // Configuramos el sonido en el canal (Android 8+)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Puedes cambiarlo luego por un icono de gota
            .setContentTitle("¡Hora de regar! 💧")
            .setContentText("$nombre tiene sed, ¡no la olvides!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri) // Sonido para versiones antiguas de Android
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}