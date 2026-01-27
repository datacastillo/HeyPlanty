package com.jucar.heyplanty

import android.app.Application
import android.net.Uri
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

    fun agregarPlanta(nombre: String, horasTexto: String, uriString: String?) {
        viewModelScope.launch {
            val horasInt = horasTexto.toIntOrNull() ?: 0

            // Si hay una imagen seleccionada, la guardamos internamente
            val rutaFinal = uriString?.let { copiarImagenInterna(it) }

            val nueva = Planta(
                nombre = nombre,
                especie = "Identificando...",
                diasEntreRiegos = horasInt,
                fechaUltimoRiego = System.currentTimeMillis(),
                imagenUri = rutaFinal // Guardamos la ruta del archivo local
            )
            plantaDao.insertPlanta(nueva)
        }
    }

    private fun copiarImagenInterna(uriString: String): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)

            // Creamos un archivo único en la carpeta interna de la app
            val nombreArchivo = "planta_${System.currentTimeMillis()}.jpg"
            val archivoDestino = File(context.filesDir, nombreArchivo)

            inputStream?.use { input ->
                FileOutputStream(archivoDestino).use { output ->
                    input.copyTo(output)
                }
            }
            archivoDestino.absolutePath // Devolvemos la ruta real del archivo
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun regarPlanta(planta: Planta) {
        viewModelScope.launch {
            plantaDao.actualizarFechaRiego(planta.id, System.currentTimeMillis())
            _eventoRiego.emit("¡${planta.nombre} ha sido regada! 💧")
        }
    }

    fun eliminarPlanta(plantaId: String) {
        viewModelScope.launch {
            plantaDao.borrarPlantaPorId(plantaId)
        }
    }
}