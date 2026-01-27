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
    val diasEntreRiegos: Int, // Representa el total de MINUTOS
    val fechaUltimoRiego: Long,
    val imagenUri: String? = null,
    val nivelDrama: Int = 3
) {
    fun tieneSed(): Boolean {
        val ahora = System.currentTimeMillis()
        val tiempoTranscurrido = ahora - fechaUltimoRiego
        // Convertimos los minutos guardados a milisegundos
        val intervaloMilis = diasEntreRiegos * 60 * 1000L
        return tiempoTranscurrido >= intervaloMilis
    }

    fun obtenerProgresoRiego(): Float {
        val ahora = System.currentTimeMillis()
        val intervaloMilis = (diasEntreRiegos * 60 * 1000L).coerceAtLeast(1L)
        val tiempoTranscurrido = ahora - fechaUltimoRiego

        val progreso = 1f - (tiempoTranscurrido.toFloat() / intervaloMilis.toFloat())
        return progreso.coerceIn(0f, 1f)
    }
}