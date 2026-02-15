package com.andresDev.puriapp.data.model

data class Cliente(
    val id: Long? = null,
    val nombreContacto: String,
    val nombreNegocio: String,
    val direccion: String,
    val referencia: String?,
    val estado: String,
    val telefono: String?,
    val fechaRegistro: String?,
    val fechaActualizacion: String?,
    val latitud: Double?,
    val longitud: Double?,
    val tieneCredito: Boolean
)

