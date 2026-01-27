package com.jucar.heyplanty

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jucar.heyplanty.data.AppDatabase
import com.jucar.heyplanty.domain.Planta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class PlantaViewModel(application: Application) : AndroidViewModel(application) {
    private val plantaDao = AppDatabase.getDatabase(application).plantaDao()
    val todasLasPlantas: Flow<List<Planta>> = plantaDao.getAllPlantas()

    private val _eventoRiego = MutableSharedFlow<String>()
    val eventoRiego = _eventoRiego.asSharedFlow()
    private var mediaPlayer: MediaPlayer? = null

    private fun programarNotificacion(plantaNombre: String, minutosTotales: Int) {
        if (minutosTotales <= 0) return

        val workRequest = OneTimeWorkRequestBuilder<NotifyWorker>()
            .setInitialDelay(minutosTotales.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf("plant_name" to plantaNombre))
            .addTag(plantaNombre)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }

    fun regarPlanta(planta: Planta) {
        viewModelScope.launch {
            ejecutarEfectosRiego()
            WorkManager.getInstance(getApplication()).cancelAllWorkByTag(planta.nombre)
            programarNotificacion(planta.nombre, planta.diasEntreRiegos)
            plantaDao.actualizarFechaRiego(planta.id, System.currentTimeMillis())
            _eventoRiego.emit("¡${planta.nombre} ha sido regada! 💧")
        }
    }

    // AHORA RECIBE HORAS Y MINUTOS COMO INT
    fun agregarPlanta(nombre: String, horas: Int, minutos: Int, uriString: String?) {
        viewModelScope.launch {
            val totalMinutos = (horas * 60) + minutos
            val minutosFinales = if (totalMinutos <= 0) 1 else totalMinutos
            val rutaFinal = uriString?.let { copiarImagenInterna(it) }

            val nueva = Planta(
                nombre = nombre,
                especie = "Identificando...",
                diasEntreRiegos = minutosFinales,
                fechaUltimoRiego = System.currentTimeMillis(),
                imagenUri = rutaFinal
            )
            plantaDao.insertPlanta(nueva)
            programarNotificacion(nombre, minutosFinales)
        }
    }

    private fun ejecutarEfectosRiego() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) { Log.e("HeyPlanty", "Error vibración: ${e.message}") }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(getApplication(), R.raw.regar_sonido)
            mediaPlayer?.let { mp ->
                mp.setOnCompletionListener { it.release() }
                mp.setVolume(1.0f, 1.0f)
                mp.start()
            }
        } catch (e: Exception) { Log.e("HeyPlanty", "Error audio: ${e.message}") }
    }

    private fun copiarImagenInterna(uriString: String): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val nombreArchivo = "planta_${System.currentTimeMillis()}.jpg"
            val archivoDestino = File(context.filesDir, nombreArchivo)
            inputStream?.use { input -> FileOutputStream(archivoDestino).use { output -> input.copyTo(output) } }
            archivoDestino.absolutePath
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun eliminarPlanta(planta: Planta) {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelAllWorkByTag(planta.nombre)
            planta.imagenUri?.let { File(it).apply { if (exists()) delete() } }
            plantaDao.borrarPlantaPorId(planta.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}