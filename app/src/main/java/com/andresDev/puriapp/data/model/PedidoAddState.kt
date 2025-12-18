package com.andresDev.puriapp.data.model

data class PedidoAddState(
    //Pedidos
    val pedidoSeleccionado: Pedido? = null,



    // Clientes
    val clientes: List<Cliente> = emptyList(),
    val clienteSeleccionado: Cliente? = null,
    val clientesFiltrados: List<Cliente> = emptyList(),

    // Vendedor
    val idVendedor: Long = 1,

    // Productos
    val productos: List<Producto> = emptyList(),
    val productosFiltrados: List<Producto> = emptyList(),
    val cantidadSeleccionada: Int = 1,
    val productoSeleccionado: Producto? = null,

    // Productos agregados al pedido
    val productosEnPedido: List<DetallePedido> = emptyList(),

    // Estados
    val isLoading: Boolean = false,
    val isLoadingProductos: Boolean = false,
    val error: String? = null,

    // Totales
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val igv: Double= 0.0
)



