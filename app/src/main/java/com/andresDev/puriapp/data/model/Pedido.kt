package com.andresDev.puriapp.data.model

data class Pedido(
    val id: Long? = null,
    val vendedor: Usuario? = null,
    val cliente: Cliente? = null,
    val visita: Visita? = null,
    val fechaPedido: String? = "",
    val subtotal: Double?=0.0,
    val igv: Double?=0.0,
    val total: Double?=0.0,
    val estado: String?="",
    val observaciones: String?="",
    val detallePedidos: List<DetallePedido> = emptyList()
)
