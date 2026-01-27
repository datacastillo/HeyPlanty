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
    val imagenUri: String? = null, // NUEVO: Dirección de la foto
    val nivelDrama: Int = 3
) {
    fun tieneSed(): Boolean {
        val ahora = System.currentTimeMillis()
        val tiempoTranscurrido = ahora - fechaUltimoRiego
        if (diasEntreRiegos == 0) return tiempoTranscurrido > 60000
        val intervaloMilis = diasEntreRiegos * 60 * 60 * 1000L
        return tiempoTranscurrido >= intervaloMilis
    }
}