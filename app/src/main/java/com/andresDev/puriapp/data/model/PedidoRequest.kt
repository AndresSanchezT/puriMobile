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
    val id: Long? = null,
    val producto: Producto,
    val cantidad: Double = 1.0,
    val precioTotal: Double,
    val precioUnitario: Double,
    val subtotal: Double
) {

    fun cantidadFormateada(): String {
        return if (cantidad % 1.0 == 0.0) {
            // Si es entero, mostrar sin decimales
            cantidad.toInt().toString()
        } else {
            // Si tiene decimales, mostrar con 1 decimal
            String.format("%.1f", cantidad)
        }
    }
}