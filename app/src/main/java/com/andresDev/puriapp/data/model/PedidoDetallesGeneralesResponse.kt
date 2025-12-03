package com.andresDev.puriapp.data.model

data class PedidoDetallesGeneralesResponse(
    val id: Long,
    val cliente: ClienteDTO?,
    val vendedor: VendedorDTO?,
    val visita: VisitaDTO?,
    val subtotal: Double,
    val igv: Double,
    val total: Double,
    val estado: String,
    val observaciones: String?,
    val detallePedidos: List<DetallePedidoDTO>
) {
    data class ClienteDTO(
        val nombre: String,
        val direccion: String,
        val telefono: String
    )
    data class VendedorDTO(
        val nombreVendedor:String
    )
    data class VisitaDTO(
        val observaciones: String?
    )

    data class DetallePedidoDTO(
        val id: Long,
        val producto: ProductoDTO,
        val cantidad: Int,
        val precioUnitario: Double,
        val subtotal: Double
    ) {
        data class ProductoDTO(
            val id: Long,
            val nombre: String,
            val precio: Double,
            val unidadMedida: String
        )
    }
}
