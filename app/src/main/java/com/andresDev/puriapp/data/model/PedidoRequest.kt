package com.andresDev.puriapp.data.model

data class PedidoRequest(
    val estado: String,
    val observaciones: String?,
    val subtotal: Double,
    val igv: Double?,
    val total: Double,
    val detallePedidos: List<DetallePedido>
)

data class DetallePedido(
    val producto: Producto,
    val cantidad: Int = 1,
    val precioTotal: Double,
    val precioUnitario: Double
) {
    val subtotal: Double
        get() = precioUnitario * cantidad
}