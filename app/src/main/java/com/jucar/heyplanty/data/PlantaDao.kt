package com.jucar.heyplanty.data

import androidx.room.*
import com.jucar.heyplanty.domain.Planta
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantaDao {
    @Query("SELECT * FROM plantas ORDER BY nombre ASC")
    fun getAllPlantas(): Flow<List<Planta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanta(planta: Planta)

    @Query("UPDATE plantas SET fechaUltimoRiego = :nuevaFecha WHERE id = :plantaId")
    suspend fun actualizarFechaRiego(plantaId: String, nuevaFecha: Long)

    // Nueva función para el Swipe to Dismiss
    @Query("DELETE FROM plantas WHERE id = :plantaId")
    suspend fun borrarPlantaPorId(plantaId: String)

    @Delete
    suspend fun deletePlanta(planta: Planta)
}