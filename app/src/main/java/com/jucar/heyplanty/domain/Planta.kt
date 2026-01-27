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
    val diasEntreRiegos: Int,
    val fechaUltimoRiego: Long,
    val imagenUri: String? = null,
    val nivelDrama: Int = 3
) {
    fun tieneSed(): Boolean {
        val ahora = System.currentTimeMillis()
        val tiempoTranscurrido = ahora - fechaUltimoRiego
        if (diasEntreRiegos == 0) return tiempoTranscurrido > 60000
        val intervaloMilis = diasEntreRiegos * 60 * 60 * 1000L
        return tiempoTranscurrido >= intervaloMilis
    }

    // NUEVA FUNCIÓN: Calcula el porcentaje de "hidratación"
    fun obtenerProgresoRiego(): Float {
        val ahora = System.currentTimeMillis()
        val intervaloMilis = diasEntreRiegos * 60 * 60 * 1000L.coerceAtLeast(1L)
        val tiempoTranscurrido = ahora - fechaUltimoRiego

        // 1.0 es recién regada, 0.0 es necesita agua ya
        val progreso = 1f - (tiempoTranscurrido.toFloat() / intervaloMilis.toFloat())
        return progreso.coerceIn(0f, 1f)
    }
}