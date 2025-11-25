package com.andresDev.puriapp.data.model

data class Producto(
    val id: Long? = null,
    val codigo: String,
    val nombre: String,
    val precio: Double,
    val stockActual: Int,
    val stockMinimo: Int,
    val cantidadFaltante: Int,
    val unidadMedida: String,
    val estado: String,
    val tipo: String,
    val descripcion: String,
    val fechaCreacion: String,
    val fechaActualzacion: String,
    val detallePedidos: List<DetallePedido> = emptyList()
)
