package com.andresDev.puriapp.data.model

data class PedidoAddState(
    //Metodos pago
    val yape: Double = 0.0,
    val plin: Double = 0.0,
    val efectivo: Double = 0.0,
    val credito: Double = 0.0,

    //Pedidos
    val pedidoSeleccionado: Pedido? = null,
    val pedidoCargado: Pedido? = null,

    // Clientes
    val clientes: List<Cliente> = emptyList(),
    val clienteSeleccionado: Cliente? = null,
    val clientesFiltrados: List<Cliente> = emptyList(),

    // Vendedor
    val idVendedor: Long = 1,

    // Productos
    val productos: List<Producto> = emptyList(),
    val productosFiltrados: List<Producto> = emptyList(),
    val cantidadSeleccionada: Double = 1.0,
    val productoSeleccionado: Producto? = null,

    // Productos agregados al pedido
    val productosEnPedido: List<DetallePedido> = emptyList(),

    // Estados
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isLoadingProductos: Boolean = false,
    val error: String? = null,

    // Totales
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val igv: Double= 0.0
)



