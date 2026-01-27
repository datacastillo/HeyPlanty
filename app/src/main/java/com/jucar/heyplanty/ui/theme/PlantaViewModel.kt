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
import androidx.work.*
import com.jucar.heyplanty.data.AppDatabase
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.domain.RiegoEvento
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

    private val _eventoRiego = MutableSharedFlow<String>(replay = 1)
    val eventoRiego = _eventoRiego.asSharedFlow()
    private var mediaPlayer: MediaPlayer? = null

    // Función que usa tu NotifyWorker.kt
    private fun programarNotificacion(planta: Planta) {
        val delayMilis = (planta.diasEntreRiegos * 60 * 1000L)
        val data = workDataOf("plant_name" to planta.nombre)

        val pedidoRiego = OneTimeWorkRequestBuilder<NotifyWorker>()
            .setInitialDelay(delayMilis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(planta.id)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            planta.id,
            ExistingWorkPolicy.REPLACE,
            pedidoRiego
        )
    }

    fun obtenerHistorial(plantaId: String): Flow<List<RiegoEvento>> {
        return plantaDao.getHistorialPorPlanta(plantaId)
    }

    fun regarPlanta(planta: Planta) {
        viewModelScope.launch {
            try {
                val ahora = System.currentTimeMillis()
                val progreso = planta.obtenerProgresoRiego()
                ejecutarEfectosRiego(esExceso = progreso >= 0.95f)

                if (progreso >= 0.95f) {
                    val nuevaSalud = (planta.salud - 15).coerceAtLeast(0)
                    plantaDao.insertarEventoRiego(RiegoEvento(plantaId = planta.id, fecha = ahora, fuePuntual = false, esSobrerego = true))
                    plantaDao.insertPlanta(planta.copy(salud = nuevaSalud, vecesSobreregada = planta.vecesSobreregada + 1))
                    _eventoRiego.emit("¡Exceso! ${planta.nombre} ya está hidratada. 🌊")
                } else {
                    val mejora = if (progreso < 0.20f) 12 else 5
                    val saludFinal = (planta.salud + mejora).coerceAtMost(100)
                    val plantaActualizada = planta.copy(fechaUltimoRiego = ahora, salud = saludFinal, vecesSobreregada = 0)

                    plantaDao.insertarEventoRiego(RiegoEvento(plantaId = planta.id, fecha = ahora, fuePuntual = (progreso < 0.25f), esSobrerego = false))
                    plantaDao.insertPlanta(plantaActualizada)
                    programarNotificacion(plantaActualizada)
                    _eventoRiego.emit("¡${planta.nombre} regada! ✨")
                }
            } catch (e: Exception) { Log.e("HeyPlanty", "Error: ${e.message}") }
        }
    }

    fun agregarPlanta(nombre: String, especie: String, h: Int, m: Int, uri: String?, consejo: String) {
        viewModelScope.launch {
            try {
                val totalMinutos = (h * 60) + m
                val rutaFinal = uri?.let { guardarImagen(it) }
                // Iniciamos al 90% para evitar el error de "Exceso" al crearla
                val fechaAjustada = System.currentTimeMillis() - (totalMinutos * 60 * 100L)

                val nueva = Planta(
                    nombre = nombre, especie = especie, consejo = consejo,
                    diasEntreRiegos = if (totalMinutos <= 0) 1 else totalMinutos,
                    fechaUltimoRiego = fechaAjustada, imagenUri = rutaFinal, salud = 100
                )
                plantaDao.insertPlanta(nueva)
                programarNotificacion(nueva)
            } catch (e: Exception) { Log.e("HeyPlanty", "Error al crear: ${e.message}") }
        }
    }

    fun eliminarPlanta(planta: Planta) {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelAllWorkByTag(planta.id)
            plantaDao.borrarHistorialDePlanta(planta.id)
            plantaDao.borrarPlantaPorId(planta.id)
            planta.imagenUri?.let { File(it).delete() }
        }
    }

    private fun guardarImagen(uriStr: String): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val ins = context.contentResolver.openInputStream(Uri.parse(uriStr))
            val file = File(context.filesDir, "planta_${System.currentTimeMillis()}.jpg")
            ins?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    private fun ejecutarEfectosRiego(esExceso: Boolean) {
        try {
            val vib = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = if (esExceso) longArrayOf(0, 40, 80, 40) else longArrayOf(0, 60)
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(getApplication(), R.raw.regar_sonido)
            mediaPlayer?.start()
        } catch (e: Exception) { }
    }
}