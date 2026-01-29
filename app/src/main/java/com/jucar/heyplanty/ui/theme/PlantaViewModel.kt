package com.jucar.heyplanty

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
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
import com.jucar.heyplanty.screens.obtenerProgresoRiego
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

// Clase de apoyo para el evento de riego
data class RiegoResult(val mensaje: String, val esExceso: Boolean)

class PlantaViewModel(application: Application) : AndroidViewModel(application) {
    private val plantaDao = AppDatabase.getDatabase(application).plantaDao()
    val todasLasPlantas: Flow<List<Planta>> = plantaDao.getAllPlantas()

    private val _eventoRiego = MutableSharedFlow<RiegoResult>(replay = 1)
    val eventoRiego = _eventoRiego.asSharedFlow()
    private var mediaPlayer: MediaPlayer? = null

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

                // VALIDACIÓN DE EXCESO (Si tiene más del 85% de humedad)
                if (progreso > 0.85f && planta.fechaUltimoRiego != 0L) {
                    ejecutarEfectosRiego(esExceso = true)
                    _eventoRiego.emit(RiegoResult("¡Cuidado! ${planta.nombre} aún tiene agua. Ya no me riegues por ahora 🌊", true))
                    return@launch
                }

                // RIEGO CORRECTO
                ejecutarEfectosRiego(esExceso = false)
                val mejora = if (progreso < 0.20f) 12 else 5
                val saludFinal = (planta.salud + mejora).coerceAtMost(100)
                val plantaActualizada = planta.copy(
                    fechaUltimoRiego = ahora,
                    salud = saludFinal,
                    vecesSobreregada = 0
                )

                plantaDao.insertarEventoRiego(
                    RiegoEvento(plantaId = planta.id, fecha = ahora, fuePuntual = true, esSobrerego = false)
                )
                plantaDao.insertPlanta(plantaActualizada)
                programarNotificacion(plantaActualizada)
                _eventoRiego.emit(RiegoResult("¡${planta.nombre} recibió su agua y está feliz! ✨🌿", false))

            } catch (e: Exception) {
                Log.e("HeyPlanty", "Error al regar: ${e.message}")
            }
        }
    }

    fun agregarPlanta(nombre: String, especie: String, totalMinutos: Int, rutaImagen: String?, consejo: String) {
        viewModelScope.launch {
            try {
                val nueva = Planta(
                    nombre = nombre,
                    especie = especie,
                    consejo = consejo,
                    diasEntreRiegos = if (totalMinutos <= 0) 1 else totalMinutos,
                    fechaUltimoRiego = 0L,
                    imagenUri = rutaImagen,
                    salud = 100
                )
                plantaDao.insertPlanta(nueva)
            } catch (e: Exception) {
                Log.e("HeyPlanty", "Error al crear: ${e.message}")
            }
        }
    }

    fun editarPlanta(plantaOriginal: Planta, nuevoNombre: String, nuevaEspecie: String, nuevosMinutos: Int, nuevaImagen: String?) {
        viewModelScope.launch {
            try {
                val plantaActualizada = plantaOriginal.copy(
                    nombre = nuevoNombre,
                    especie = nuevaEspecie,
                    diasEntreRiegos = nuevosMinutos,
                    imagenUri = nuevaImagen
                )
                plantaDao.insertPlanta(plantaActualizada)
                programarNotificacion(plantaActualizada)
                _eventoRiego.emit(RiegoResult("¡${nuevoNombre} ha sido actualizada! 📝", false))
            } catch (e: Exception) {
                Log.e("HeyPlanty", "Error al editar: ${e.message}")
            }
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

    private fun ejecutarEfectosRiego(esExceso: Boolean) {
        try {
            val vib = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibración más larga/doble para el exceso
                val pattern = if (esExceso) longArrayOf(0, 100, 50, 100) else longArrayOf(0, 50)
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(getApplication(), R.raw.regar_sonido)
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("HeyPlanty", "Error en efectos: ${e.message}")
        }
    }
}