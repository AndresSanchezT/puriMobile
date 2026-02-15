package com.andresDev.puriapp.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.model.DetallePedido
import com.andresDev.puriapp.data.model.PedidoAddState
import com.andresDev.puriapp.data.model.Producto
import com.andresDev.puriapp.data.repository.PedidoRepository
import com.andresDev.puriapp.data.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DetallePedidoViewModel @Inject constructor(
    private val pedidoRepository: PedidoRepository,
    private val productoRepository: ProductoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PedidoAddState())
    val uiState: StateFlow<PedidoAddState> = _uiState.asStateFlow()

    private val _searchQueryProducto = MutableStateFlow("")

    init {
        cargarProductos()
        setupSearchDebounce()
    }

    fun cargarPedido(pedidoId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val pedido = pedidoRepository.obtenerPedidoPorId(pedidoId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pedidoCargado = pedido,
                        productosEnPedido = pedido.detallePedidos,
                        subtotal = pedido.subtotal ?: 0.0,
                        igv = pedido.igv ?: 0.0,
                        total = pedido.total ?: 0.0
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar pedido"
                    )
                }
            }
        }
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProductos = true) }

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

    fun actualizarPedido() {
        val pedidoCargado = _uiState.value.pedidoCargado ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }

            try {
                
                // Crear el pedido actualizado con los nuevos valores
                val state = _uiState.value

                val pedidoActualizado = pedidoCargado.copy(
                    detallePedidos = state.productosEnPedido,
                    subtotal = state.subtotal,
                    igv = state.igv,
                    total = state.total,
                    yape = state.yape,
                    plin = state.plin,
                    efectivo = state.efectivo,
                    credito = state.credito
                )

                // Llamar al repositorio para actualizar
                val resultado = pedidoRepository.actualizarPedido(
                    id = pedidoCargado.id ?: 0L,
                    pedido = pedidoActualizado
                )

                resultado.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isUpdating = false,
                                error = "success"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isUpdating = false,
                                error = exception.message ?: "Error al actualizar pedido"
                            )
                        }
                    }
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Error al actualizar: ${e.message}"
                    )
                }
            }
        }
    }

    fun actualizarPrecioUnitario(productoId: Long?, nuevoPrecio: Double) {
        val productosActualizados = _uiState.value.productosEnPedido.map { productoPedido ->
            if (productoPedido.producto.id == productoId) {
                val nuevoSubtotal = nuevoPrecio * productoPedido.cantidad
                productoPedido.copy(
                    precioUnitario = nuevoPrecio,
                    precioTotal = nuevoPrecio * productoPedido.cantidad,
                    subtotal = nuevoSubtotal
                )
            } else {
                productoPedido
            }
        }
        actualizarProductosYTotales(productosActualizados)
    }

    fun actualizarCantidad(productoId: Long?, nuevaCantidad: Double) {
        if (nuevaCantidad <= 0) {
            eliminarProducto(productoId)
            return
        }

        val productosActualizados = _uiState.value.productosEnPedido.map { productoPedido ->
            if (productoPedido.producto.id == productoId) {
                val nuevoSubtotal = productoPedido.precioUnitario * nuevaCantidad
                productoPedido.copy(
                    cantidad = nuevaCantidad,
                    precioTotal = productoPedido.precioUnitario * nuevaCantidad,
                    subtotal = nuevoSubtotal
                )
            } else {
                productoPedido
            }
        }
        actualizarProductosYTotales(productosActualizados)
    }

    fun actualizarSubtotal(productoId: Long?, nuevoSubtotal: Double) {
        val productosActualizados = _uiState.value.productosEnPedido.map { productoPedido ->
            if (productoPedido.producto.id == productoId) {
                // Calcular nuevo precio unitario basado en el subtotal deseado

                productoPedido.copy(
                    subtotal = nuevoSubtotal
                )
            } else {
                productoPedido
            }
        }
        actualizarProductosYTotales(productosActualizados)
    }

    fun actualizarPagos(
        yape: Double,
        plin: Double,
        efectivo: Double,
        credito: Double
    ) {
        _uiState.update {
            it.copy(
                yape = yape,
                plin = plin,
                efectivo = efectivo,
                credito = credito
            )
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

    private fun setupSearchDebounce() {
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
    // ============ MANEJO DE PRODUCTOS EN PEDIDO ============

    fun agregarProducto(
        producto: Producto,
        cantidad: Double = 1.0,
        precioTotal: Double,
        precioUnitario: Double  // ✅ NUEVO parámetro
    ) {
        val productosActuales = _uiState.value.productosEnPedido.toMutableList()
        val productoExistente = productosActuales.find { it.producto.id == producto.id }

        val subtotal = precioTotal

        if (productoExistente != null) {
            val index = productosActuales.indexOf(productoExistente)
            productosActuales[index] = productoExistente.copy(
                cantidad = productoExistente.cantidad + cantidad,
                precioTotal = productoExistente.precioTotal + precioTotal,
                subtotal = productoExistente.subtotal + subtotal
            )
        } else {
            productosActuales.add(0,
                DetallePedido(
                    producto = producto,
                    cantidad = cantidad,
                    precioTotal = precioTotal,
                    precioUnitario = precioUnitario,  // ✅ Usar parámetro
                    subtotal = subtotal
                )
            )
        }

        actualizarProductosYTotales(productosActuales)

        _uiState.update {
            it.copy(productoSeleccionado = null)
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
