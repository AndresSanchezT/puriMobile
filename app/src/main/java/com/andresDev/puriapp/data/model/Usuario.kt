package com.andresDev.puriapp.data.model

data class Usuario(
    val id: Long? = null,
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val telefono: String?,
    val fechaCreacion: String?,
    val fechaActualizacion: String?,
    val visitas: List<Visita> = emptyList(),
    val pedidos: List<Pedido> = emptyList()
)
