package com.jucar.heyplanty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jucar.heyplanty.data.AppDatabase
import com.jucar.heyplanty.domain.Planta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PlantaViewModel(application: Application) : AndroidViewModel(application) {
    private val plantaDao = AppDatabase.getDatabase(application).plantaDao()
    val todasLasPlantas: Flow<List<Planta>> = plantaDao.getAllPlantas()

    private val _eventoRiego = MutableSharedFlow<String>()
    val eventoRiego = _eventoRiego.asSharedFlow()

    fun agregarPlanta(nombre: String, diasTexto: String) {
        viewModelScope.launch {
            val diasInt = diasTexto.toIntOrNull() ?: 0
            val nueva = Planta(
                nombre = nombre,
                especie = "Identificando...",
                diasEntreRiegos = diasInt,
                fechaUltimoRiego = System.currentTimeMillis()
            )
            plantaDao.insertPlanta(nueva)
        }
    }

    fun regarPlanta(planta: Planta) {
        viewModelScope.launch {
            val nuevaFecha = System.currentTimeMillis()
            // Primero actualizamos en la DB
            plantaDao.actualizarFechaRiego(planta.id, nuevaFecha)
            // Emitimos el mensaje después del cambio
            _eventoRiego.emit("¡${planta.nombre} ha sido regada! 💧")
        }
    }

    fun eliminarPlanta(plantaId: String) {
        viewModelScope.launch {
            plantaDao.borrarPlantaPorId(plantaId)
        }
    }
}