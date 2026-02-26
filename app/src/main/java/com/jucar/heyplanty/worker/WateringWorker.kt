package com.jucar.heyplanty.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.jucar.heyplanty.MainActivity
import com.jucar.heyplanty.R
import com.jucar.heyplanty.data.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WateringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Cortesía Nocturna: No notificar entre 9 PM y 8 AM
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour !in 8..21) return Result.success()

        val db = AppDatabase.getDatabase(applicationContext)
        val plantas = db.plantaDao().getAllPlantas().first()

        plantas.forEach { planta ->
            // Si la planta tiene sed (progreso <= 0) y ha sido regada al menos una vez
            if (planta.obtenerProgresoRiego() <= 0f && planta.fechaUltimoRiego > 0L) {
                enviarNotificacion(planta.nombre)
            }
        }

        return Result.success()
    }

    private fun enviarNotificacion(nombrePlanta: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "heyplanty_watering"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Recordatorios de Riego", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos cuando tus plantas necesitan agua"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            nombrePlanta.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Idealmente un icono de gota
            .setContentTitle("¡Tengo sed! 💧")
            .setContentText("Hola, soy $nombrePlanta. ¿Podrías regarme un poquito?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        manager.notify(nombrePlanta.hashCode(), notification)
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WateringWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "WateringCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
