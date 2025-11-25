package com.andresDev.puriapp.data.model

data class DetallePedido(
    val id: Long? = null,
    val pedido: Pedido,
    val producto: Producto,
    val cantidad: Int,
    val precioUnitario: Double?,
    val subtotal: Double?
)
