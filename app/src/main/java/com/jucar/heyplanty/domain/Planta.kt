package com.jucar.heyplanty.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "plantas")
data class Planta(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val especie: String,
    val consejo: String = "",
    val diasEntreRiegos: Int,
    val fechaUltimoRiego: Long,
    val imagenUri: String? = null,
    val nivelDrama: Int = 3,
    val salud: Int = 100,
    val vecesSobreregada: Int = 0
) {
    fun obtenerProgresoRiego(): Float {
        val ahora = System.currentTimeMillis()
        val intervaloMilis = (diasEntreRiegos * 60 * 1000L).coerceAtLeast(1L)
        val tiempoTranscurrido = ahora - fechaUltimoRiego
        if (tiempoTranscurrido <= 0) return 1.0f
        val progreso = (intervaloMilis - tiempoTranscurrido).toFloat() / intervaloMilis.toFloat()
        return progreso.coerceIn(0f, 1f)
    }

    fun esCritica(): Boolean = obtenerProgresoRiego() < 0.2f
    fun estaEnferma(): Boolean = salud < 40
}

@Entity(tableName = "historial_riego")
data class RiegoEvento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val plantaId: String,
    val fecha: Long,
    val fuePuntual: Boolean,
    val esSobrerego: Boolean = false // Este campo corrige el error 'esSobrerego' de la imagen
)