package com.jucar.heyplanty.data

import androidx.room.*
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.domain.RiegoEvento
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantaDao {
    @Query("SELECT * FROM plantas ORDER BY nombre ASC")
    fun getAllPlantas(): Flow<List<Planta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanta(planta: Planta)

    @Query("UPDATE plantas SET fechaUltimoRiego = :nuevaFecha WHERE id = :plantaId")
    suspend fun actualizarFechaRiego(plantaId: String, nuevaFecha: Long)

    @Query("DELETE FROM plantas WHERE id = :plantaId")
    suspend fun borrarPlantaPorId(plantaId: String)

    // CONSULTAS PARA EL HISTORIAL
    @Insert
    suspend fun insertarEventoRiego(evento: RiegoEvento)

    @Query("SELECT * FROM historial_riego WHERE plantaId = :plantaId ORDER BY fecha DESC LIMIT 5")
    fun getHistorialPorPlanta(plantaId: String): Flow<List<RiegoEvento>>

    @Query("DELETE FROM historial_riego WHERE plantaId = :plantaId")
    suspend fun borrarHistorialDePlanta(plantaId: String)
}