package com.jucar.heyplanty.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "plantas")
data class Planta(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // El ID es un STRING (UUID)
    val nombre: String,
    val especie: String,
    val diasEntreRiegos: Int,
    val fechaUltimoRiego: Long,
    val nivelDrama: Int = 3
)