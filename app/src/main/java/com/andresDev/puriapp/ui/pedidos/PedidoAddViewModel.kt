package com.andresDev.puriapp.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.data.model.PedidoAddState
import com.andresDev.puriapp.data.model.PedidoRequest
import com.andresDev.puriapp.data.model.Producto
import com.andresDev.puriapp.data.repository.ClienteRepository
import com.andresDev.puriapp.data.repository.PedidoRepository
import com.andresDev.puriapp.data.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class PedidoAddViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository,
    private val productoRepository: ProductoRepository,
    private val pedidoRepository: PedidoRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PedidoAddState())
    val uiState: StateFlow<PedidoAddState> = _uiState.asStateFlow()

    // Flows para búsqueda con debounce
    private val _searchQueryCliente = MutableStateFlow("")
    private val _searchQueryProducto = MutableStateFlow("")

    init {
        cargarClientes()
        cargarProductos()
        setupSearchDebounce()
        cargarIdVendedor()
    }

    private fun cargarIdVendedor() {
        val idVendedor = tokenManager.getUserId()

        if (idVendedor != 0L) {
            _uiState.update {
                it.copy(idVendedor = idVendedor)
            }
        }
    }

    private fun setupSearchDebounce() {
        // Debounce para clientes
        viewModelScope.launch {
            _searchQueryCliente
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    ejecutarFiltradoClientes(query)
                }
        }

        // Debounce para productos
        viewModelScope.launch {
            _searchQueryProducto
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    ejecutarFiltradoProductos(query)
                }
        }
    }

    // ============ CLIENTES ============

    fun cargarClientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val clientes = clienteRepository.obtenerClientes()
                _uiState.update {
                    it.copy(
                        clientes = clientes,
                        clientesFiltrados = clientes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Error al cargar clientes: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun filtrarClientes(query: String) {
        _searchQueryCliente.value = query
    }

    private fun ejecutarFiltradoClientes(query: String) {
        val clientesFiltrados = if (query.isEmpty()) {
            _uiState.value.clientes
        } else {
            clienteRepository.buscarEnCache(query)
        }

        _uiState.update {
            it.copy(clientesFiltrados = clientesFiltrados)
        }
    }

    fun seleccionarCliente(cliente: Cliente) {
        _uiState.update {
            it.copy(clienteSeleccionado = cliente)
        }
    }

    // ============ PRODUCTOS ============

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProductos = true, error = null) }

            try {
                val productos = productoRepository.obtenerProductos()
                _uiState.update {
                    it.copy(
                        productos = productos,
                        productosFiltrados = productos,
                        isLoadingProductos = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Error al cargar productos: ${e.message}",
                        isLoadingProductos = false
                    )
                }
            }
        }
    }

    fun filtrarProductos(query: String) {
        _searchQueryProducto.value = query
    }

    private fun ejecutarFiltradoProductos(query: String) {
        val productosFiltrados = if (query.isEmpty()) {
            _uiState.value.productos
        } else {
            productoRepository.buscarEnCache(query)
        }

        _uiState.update {
            it.copy(productosFiltrados = productosFiltrados)
        }
    }

    fun seleccionarProducto(producto: Producto) {
        _uiState.update {
            it.copy(productoSeleccionado = producto)
        }
    }

    // ============ MANEJO DE PRODUCTOS EN PEDIDO ============

    fun agregarProducto(
        producto: Producto,
        cantidad: Double = 1.0,
        precioTotal: Double
    ) {
        val productosActuales = _uiState.value.productosEnPedido.toMutableList()

        val productoExistente = productosActuales.find { it.producto.id == producto.id }

        val subtotal = precioTotal

        if (productoExistente != null) {
            val index = productosActuales.indexOf(productoExistente)
            productosActuales[index] = productoExistente.copy(
                cantidad = productoExistente.cantidad + cantidad,
                precioTotal = productoExistente.precioTotal + precioTotal
            )
        } else {
            productosActuales.add(
                DetallePedido(
                    producto = producto,
                    cantidad = cantidad,
                    precioTotal = precioTotal,
                    precioUnitario = precioTotal/cantidad,
                    subtotal = subtotal
                )
            )
        }

        actualizarProductosYTotales(productosActuales)

        _uiState.update {
            it.copy(productoSeleccionado = null)
        }
    }

    fun actualizarCantidad(productoId: Long?, nuevaCantidad: Double) {
        if (nuevaCantidad <= 0) {
            eliminarProducto(productoId)
            return
        }

        val productosActualizados = _uiState.value.productosEnPedido.map { productoPedido ->
            if (productoPedido.producto.id == productoId) {
                val nuevoSubtotal = productoPedido.precioUnitario * nuevaCantidad
                productoPedido.copy(cantidad = nuevaCantidad,subtotal = nuevoSubtotal)
            } else {
                productoPedido
            }
        }

        actualizarProductosYTotales(productosActualizados)
    }

    fun incrementarCantidad(productoId: Long?) {
        val productoPedido = _uiState.value.productosEnPedido.find {
            it.producto.id == productoId
        }
        productoPedido?.let {
            actualizarCantidad(productoId, it.cantidad + 1)
        }
    }

    fun decrementarCantidad(productoId: Long?) {
        val productoPedido = _uiState.value.productosEnPedido.find {
            it.producto.id == productoId
        }
        productoPedido?.let {
            actualizarCantidad(productoId, it.cantidad - 1)
        }
    }

    fun eliminarProducto(productoId: Long?) {
        val productosActualizados = _uiState.value.productosEnPedido.filter {
            it.producto.id != productoId
        }
        actualizarProductosYTotales(productosActualizados)
    }

    private fun actualizarProductosYTotales(productos: List<DetallePedido>) {
        val subtotal = productos.sumOf { it.subtotal }
        val total = subtotal // Aquí puedes agregar descuentos, impuestos, etc.

        _uiState.update {
            it.copy(
                productosEnPedido = productos,
                subtotal = subtotal,
                total = total
            )
        }
    }

    fun guardarPedido(observacion: String) {
        val state = _uiState.value

        // Validaciones
        if (state.clienteSeleccionado == null) {
            _uiState.update { it.copy(error = "Debe seleccionar un cliente") }
            return
        }

        if (state.productosEnPedido.isEmpty()) {
            _uiState.update { it.copy(error = "Debe agregar al menos un producto") }
            return
        }

        if (state.idVendedor == 0L) {
            _uiState.update { it.copy(error = "Error: ID de vendedor no configurado") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }


            val pedidoRequest = PedidoRequest(
                subtotal = state.subtotal,
                igv = state.igv,
                total = state.total,
                estado = "registrado",
                observaciones = observacion,
                detallePedidos = state.productosEnPedido
            )

            val result = pedidoRepository.registrarPedido(
                idCliente = state.clienteSeleccionado.id!!,
                idVendedor = state.idVendedor,
                pedidoRequest = pedidoRequest
            )

            result.fold(
                onSuccess = { pedido ->
                    _uiState.update {
                        PedidoAddState(error = "success")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al crear el pedido"
                        )
                    }
                }
            )
        }
    }

    // ============ UTILIDADES ============

    fun limpiarError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    fun limpiarSelecciones() {
        _uiState.update {
            it.copy(
                clienteSeleccionado = null,
                productoSeleccionado = null
            )
        }
    }
    fun limpiarProductoSeleccionado() {
        _uiState.update {
            it.copy(
                productoSeleccionado = null
            )
        }
    }
}