package com.andresDev.puriapp.data.model

data class PedidoAddState(
    // Clientes
    val clientes: List<Cliente> = emptyList(),
    val clienteSeleccionado: Cliente? = null,
    val clientesFiltrados: List<Cliente> = emptyList(),

    // Vendedor
    val idVendedor: Long = 1,

    // Productos
    val productos: List<Producto> = emptyList(),
    val productosFiltrados: List<Producto> = emptyList(),
    val productoSeleccionado: Producto? = null,

    // Productos agregados al pedido
    val productosEnPedido: List<DetallePedido> = emptyList(),

    // Estados
    val isLoading: Boolean = false,
    val isLoadingProductos: Boolean = false,
    val error: String? = null,
    val observaciones: String? = "sin observaciones",

    // Totales
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val igv: Double= 0.0
)
// Clase para manejar productos en el pedido con su cantidad
//data class ProductoPedido(
//    val producto: Producto,
//    val cantidad: Int = 1
//) {
//    val precioTotal: Double
//        get() = producto.precio * cantidad
//}



