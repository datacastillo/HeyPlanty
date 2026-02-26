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
        val delayMilis = (planta.minutosEntreRiegos * 60 * 1000L)
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
                val progreso = planta.obtenerProgresoRiego() // Progress before watering

                // --- 1. PENALIZACIÓN POR SOBRERIEGO ---
                if (progreso > 0.85f && planta.fechaUltimoRiego != 0L) {
                    ejecutarEfectosRiego(esExceso = true)
                    val nuevaSalud = (planta.salud - 15).coerceAtLeast(0) // Reduce health for overwatering
                    val vecesSobreregadas = planta.vecesSobreregada + 1
                    val plantaActualizada = planta.copy(
                        salud = nuevaSalud,
                        vecesSobreregada = vecesSobreregadas,
                        // No actualizamos fecha de riego para no "premiar" el sobreriego
                    )
                    plantaDao.insertPlanta(plantaActualizada)
                    plantaDao.insertarEventoRiego(
                        RiegoEvento(plantaId = planta.id, fecha = ahora, fuePuntual = false, esSobrerego = true)
                    )
                    _eventoRiego.emit(RiegoResult("¡Cuidado! ${planta.nombre} aún tiene agua. El exceso de riego daña sus raíces.", true))
                    return@launch
                }

                // --- 2. CÁLCULO DE VITALIDAD POR RETRASO (SI APLICA) ---
                val intervaloMilis = TimeUnit.MINUTES.toMillis(planta.minutosEntreRiegos.toLong())
                val proximoRiegoEsperado = planta.fechaUltimoRiego + intervaloMilis
                val retrasoMilis = (ahora - proximoRiegoEsperado).coerceAtLeast(0L)
                var saludActual = planta.salud

                if (retrasoMilis > 0 && planta.fechaUltimoRiego != 0L) {
                    // Penalizar 1 punto de salud por cada 12 horas de retraso
                    val horasDeRetraso = TimeUnit.MILLISECONDS.toHours(retrasoMilis)
                    val saludPerdida = (horasDeRetraso / 12).toInt()
                    saludActual = (planta.salud - saludPerdida).coerceAtLeast(0)
                }

                // --- 3. RECUPERACIÓN DE VITALIDAD CON EL RIEGO ---
                // Si estaba crítica, recupera más. Si estaba bien, recupera un poco.
                val recuperacion = if (progreso < 0.2f) 25 else 10
                val saludFinal = (saludActual + recuperacion).coerceAtMost(100)

                // --- 4. ACTUALIZACIÓN FINAL DE LA PLANTA ---
                ejecutarEfectosRiego(esExceso = false)
                val plantaActualizada = planta.copy(
                    fechaUltimoRiego = ahora,
                    salud = saludFinal,
                    vecesSobreregada = 0 // Reset counter on correct watering
                )

                plantaDao.insertarEventoRiego(
                    RiegoEvento(plantaId = planta.id, fecha = ahora, fuePuntual = progreso >= 0.2f, esSobrerego = false)
                )
                plantaDao.insertPlanta(plantaActualizada)
                programarNotificacion(plantaActualizada)
                _eventoRiego.emit(RiegoResult("¡${planta.nombre} recibió su agua y está feliz! ✨🌿", false))

            } catch (e: Exception) {
                Log.e("HeyPlanty", "Error al regar: ${e.message}")
            }
        }
    }

    fun agregarPlanta(nombre: String, especie: String, totalMinutos: Int, rutaImagen: String?, tipoDeLuz: String, tipoDeSuelo: String, notas: String) {
        viewModelScope.launch {
            try {
                val nueva = Planta(
                    nombre = nombre,
                    especie = especie,
                    minutosEntreRiegos = if (totalMinutos <= 0) 1 else totalMinutos,
                    fechaUltimoRiego = 0L,
                    imagenUri = rutaImagen,
                    salud = 100,
                    tipoDeLuz = tipoDeLuz,
                    tipoDeSuelo = tipoDeSuelo,
                    notas = notas
                )
                plantaDao.insertPlanta(nueva)
            } catch (e: Exception) {
                Log.e("HeyPlanty", "Error al crear: ${e.message}")
            }
        }
    }

    fun editarPlanta(plantaOriginal: Planta, nuevoNombre: String, nuevaEspecie: String, nuevosMinutos: Int, nuevaImagen: String?, nuevoTipoDeLuz: String, nuevoTipoDeSuelo: String, nuevasNotas: String) {
        viewModelScope.launch {
            try {
                val plantaActualizada = plantaOriginal.copy(
                    nombre = nuevoNombre,
                    especie = nuevaEspecie,
                    minutosEntreRiegos = nuevosMinutos,
                    imagenUri = nuevaImagen,
                    tipoDeLuz = nuevoTipoDeLuz,
                    tipoDeSuelo = nuevoTipoDeSuelo,
                    notas = nuevasNotas
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
