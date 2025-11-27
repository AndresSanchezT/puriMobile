package com.andresDev.puriapp.data.model

data class Visita(
    val id: Long? = null,
    val vendedor: Usuario,
    val cliente: Cliente,
    val fecha: String?,
    val estado: String?,
    val observaciones: String?
)
