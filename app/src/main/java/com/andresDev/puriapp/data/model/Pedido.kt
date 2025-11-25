package com.andresDev.puriapp.data.model

data class Pedido(
    val id: Long? = null,
    val vendedor: Usuario,
    val cliente: Cliente,
    val visita: Visita,
    val fechaPedido: String?,
    val subtotal: Double?,
    val igv: Double?,
    val total: Double?,
    val estado: String?,
    val observaciones: String?,
    val detallePedido: List<DetallePedido> = emptyList()
)
