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

    // SOLUCIÓN: plantaId DEBE ser String para que coincida con la clase Planta
    @Query("UPDATE plantas SET fechaUltimoRiego = :nuevaFecha WHERE id = :plantaId")
    suspend fun actualizarFechaRiego(plantaId: String, nuevaFecha: Long)

    @Delete
    suspend fun deletePlanta(planta: Planta)
}