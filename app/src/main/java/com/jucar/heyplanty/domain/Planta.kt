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
    val nivelDrama: Int = 3
) {
    // FUNCIÓN NUEVA: Calcula si la planta necesita agua ya mismo
    fun tieneSed(): Boolean {
        val tiempoTranscurrido = System.currentTimeMillis() - fechaUltimoRiego
        val diasEnMilisegundos = diasEntreRiegos * 24 * 60 * 60 * 1000L
        return tiempoTranscurrido >= diasEnMilisegundos
    }
}