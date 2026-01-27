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
import com.jucar.heyplanty.data.AppDatabase
import com.jucar.heyplanty.domain.Planta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class PlantaViewModel(application: Application) : AndroidViewModel(application) {
    private val plantaDao = AppDatabase.getDatabase(application).plantaDao()
    val todasLasPlantas: Flow<List<Planta>> = plantaDao.getAllPlantas()

    private val _eventoRiego = MutableSharedFlow<String>()
    val eventoRiego = _eventoRiego.asSharedFlow()

    private var mediaPlayer: MediaPlayer? = null

    // --- LÓGICA DE RIEGO ---
    fun regarPlanta(planta: Planta) {
        viewModelScope.launch {
            ejecutarEfectosRiego()
            plantaDao.actualizarFechaRiego(planta.id, System.currentTimeMillis())
            _eventoRiego.emit("¡${planta.nombre} ha sido regada! 💧")
        }
    }

    private fun ejecutarEfectosRiego() {
        // 1. Vibración con soporte para versiones antiguas y nuevas
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26+
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // API < 26 (Deprecado pero necesario para soporte)
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.e("HeyPlanty", "Error vibración: ${e.message}")
        }

        // 2. Sonido
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(getApplication(), R.raw.regar_sonido)
            mediaPlayer?.let { mp ->
                mp.setOnCompletionListener { it.release() }
                mp.setVolume(1.0f, 1.0f)
                mp.start()
            }
        } catch (e: Exception) {
            Log.e("HeyPlanty", "Error al reproducir audio: ${e.message}")
        }
    }

    // --- RESTO DE FUNCIONES ---
    fun agregarPlanta(nombre: String, horasTexto: String, uriString: String?) {
        viewModelScope.launch {
            val horasInt = horasTexto.toIntOrNull() ?: 0
            val rutaFinal = uriString?.let { copiarImagenInterna(it) }

            val nueva = Planta(
                nombre = nombre,
                especie = "Identificando...",
                diasEntreRiegos = horasInt,
                fechaUltimoRiego = System.currentTimeMillis(),
                imagenUri = rutaFinal
            )
            plantaDao.insertPlanta(nueva)
        }
    }

    private fun copiarImagenInterna(uriString: String): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val nombreArchivo = "planta_${System.currentTimeMillis()}.jpg"
            val archivoDestino = File(context.filesDir, nombreArchivo)

            inputStream?.use { input ->
                FileOutputStream(archivoDestino).use { output ->
                    input.copyTo(output)
                }
            }
            archivoDestino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun eliminarPlanta(planta: Planta) {
        viewModelScope.launch {
            planta.imagenUri?.let { ruta ->
                val archivo = File(ruta)
                if (archivo.exists()) archivo.delete()
            }
            plantaDao.borrarPlantaPorId(planta.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}