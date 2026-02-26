package com.jucar.heyplanty.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.util.concurrent.TimeUnit

@Entity(tableName = "plantas")
data class Planta(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val especie: String,
    val consejo: String = "",
    val tipoDeLuz: String = "",
    val tipoDeSuelo: String = "",
    val notas: String = "",
    val minutosEntreRiegos: Int,
    val fechaUltimoRiego: Long,
    val imagenUri: String? = null,
    val nivelDrama: Int = 3,
    val salud: Int = 100,
    val vecesSobreregada: Int = 0
) {
    fun obtenerProgresoRiego(): Float {
        if (fechaUltimoRiego == 0L) return 1.0f // Nueva planta, progreso lleno
        val ahora = System.currentTimeMillis()
        val intervaloMilis = TimeUnit.MINUTES.toMillis(minutosEntreRiegos.toLong()).coerceAtLeast(1L)
        val tiempoTranscurrido = ahora - fechaUltimoRiego
        if (tiempoTranscurrido <= 0) return 1.0f
        val progreso = (intervaloMilis - tiempoTranscurrido).toFloat() / intervaloMilis.toFloat()
        return progreso.coerceIn(0f, 1f)
    }

    fun obtenerTiempoRestanteFormateado(): String {
        if (fechaUltimoRiego == 0L) {
            return "¡Lista para empezar!"
        }
        val ahora = System.currentTimeMillis()
        val intervaloMilis = TimeUnit.MINUTES.toMillis(minutosEntreRiegos.toLong())
        val proximoRiego = fechaUltimoRiego + intervaloMilis
        val tiempoRestanteMilis = (proximoRiego - ahora).coerceAtLeast(0L)

        if (tiempoRestanteMilis <= 0) {
            return "¡Regar ahora!"
        }

        val dias = TimeUnit.MILLISECONDS.toDays(tiempoRestanteMilis)
        val horas = TimeUnit.MILLISECONDS.toHours(tiempoRestanteMilis) % 24
        val minutos = TimeUnit.MILLISECONDS.toMinutes(tiempoRestanteMilis) % 60
        val segundos = TimeUnit.MILLISECONDS.toSeconds(tiempoRestanteMilis) % 60

        return when {
            dias > 0 -> String.format("%d d y %d h", dias, horas)
            horas > 0 -> String.format("%d h y %d m", horas, minutos)
            minutos > 0 -> String.format("%d m y %d s", minutos, segundos)
            else -> String.format("%d s", segundos)
        }
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
    val esSobrerego: Boolean = false
)
