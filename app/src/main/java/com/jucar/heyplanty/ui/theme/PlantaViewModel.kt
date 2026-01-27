package com.jucar.heyplanty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jucar.heyplanty.data.AppDatabase
import com.jucar.heyplanty.domain.Planta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PlantaViewModel(application: Application) : AndroidViewModel(application) {
    private val plantaDao = AppDatabase.getDatabase(application).plantaDao()
    val todasLasPlantas: Flow<List<Planta>> = plantaDao.getAllPlantas()

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

    fun regarPlanta(plantaId: String) {
        viewModelScope.launch {
            plantaDao.actualizarFechaRiego(plantaId, System.currentTimeMillis())
        }
    }

    fun eliminarPlanta(plantaId: String) {
        viewModelScope.launch {
            plantaDao.borrarPlantaPorId(plantaId)
        }
    }
}