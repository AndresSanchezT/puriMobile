package com.andresDev.puriapp.ui.pedidos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.data.repository.PedidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PedidoViewModel @Inject constructor(
    private val pedidoRepository: PedidoRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // ✅ NUEVO: Lista completa sin filtrar
    private val _pedidosCompletos = MutableStateFlow<List<PedidoListaReponse>>(emptyList())

    private val _pedidoEliminadoState = MutableStateFlow<EliminacionState>(EliminacionState.Idle)
    val pedidoEliminadoState: StateFlow<EliminacionState> = _pedidoEliminadoState.asStateFlow()

    // Lista visible (filtrada o completa)
    private val _pedidos = MutableStateFlow<List<PedidoListaReponse>>(emptyList())
    val pedidos = _pedidos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _pedidoEntregadoState = MutableStateFlow<EntregaState>(EntregaState.Idle)
    val pedidoEntregadoState: StateFlow<EntregaState> = _pedidoEntregadoState.asStateFlow()

    // ✅ NUEVO: Estado para guardar orden
    private val _ordenGuardadoState = MutableStateFlow<OrdenGuardadoState>(OrdenGuardadoState.Idle)
    val ordenGuardadoState: StateFlow<OrdenGuardadoState> = _ordenGuardadoState.asStateFlow()

    private val _efectivoDelDia = MutableStateFlow(0.0)
    val efectivoDelDia: StateFlow<Double> = _efectivoDelDia.asStateFlow()

    private val _efectivoLoading = MutableStateFlow(false)
    val efectivoLoading: StateFlow<Boolean> = _efectivoLoading.asStateFlow()

    val isAdmin: Boolean
        get() = tokenManager.getUserRole()?.equals("administrador", ignoreCase = true) == true

    init {
        Log.d("PedidoViewModel", "🚀 init() - Cargando pedidos de hoy inicialmente")
        cargarPedidosHoy()
        actualizarEfectivoDelDia()
    }

    // ✅ NUEVO: Actualizar orden en el servidor
    fun actualizarOrdenPedidos(pedidos: List<PedidoListaReponse>) {
        viewModelScope.launch {
            try {
                _ordenGuardadoState.value = OrdenGuardadoState.Loading

                Log.d("PedidoViewModel", "💾 Guardando nuevo orden de ${pedidos.size} pedidos")

                val result = pedidoRepository.actualizarOrdenPedidos(pedidos)

                result.onSuccess {
                    Log.d("PedidoViewModel", "✅ Orden guardado exitosamente en el servidor")

                    // ✅ Actualizar las listas locales
                    _pedidosCompletos.value = pedidos
                    _pedidos.value = pedidos

                    _ordenGuardadoState.value = OrdenGuardadoState.Success

                }.onFailure { error ->
                    Log.e("PedidoViewModel", "❌ Error al guardar orden: ${error.message}")
                    _ordenGuardadoState.value = OrdenGuardadoState.Error(
                        error.message ?: "Error desconocido"
                    )
                }

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "Error al actualizar orden", e)
                _ordenGuardadoState.value = OrdenGuardadoState.Error(
                    e.message ?: "Error desconocido"
                )
            }
        }
    }

    // ✅ MODIFICADO: Guardar en _pedidosCompletos
    fun cargarPedidosHoy() {
        viewModelScope.launch {
            Log.d("PedidoViewModel", "📥 cargarPedidosHoy()")
            _loading.value = true

            try {
                val lista = pedidoRepository.obtenerListaPedidosHoy()

                Log.d("PedidoViewModel", "✅ Pedidos de HOY recibidos: ${lista.size}")

                // ✅ CAMBIO: Guardar en ambas listas
                _pedidosCompletos.value = lista
                _pedidos.value = lista

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "❌ Error cargando pedidos de hoy: ${e.message}", e)
                _pedidosCompletos.value = emptyList()
                _pedidos.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    // ✅ MODIFICADO: Guardar en _pedidosCompletos
    fun cargarPedidosManana() {
        viewModelScope.launch {
            Log.d("PedidoViewModel", "📥 cargarPedidosManana()")
            _loading.value = true

            try {
                val lista = pedidoRepository.obtenerListaPedidosManana()

                Log.d("PedidoViewModel", "✅ Pedidos de MAÑANA recibidos: ${lista.size}")

                // ✅ CAMBIO: Guardar en ambas listas
                _pedidosCompletos.value = lista
                _pedidos.value = lista

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "❌ Error cargando pedidos de mañana: ${e.message}", e)
                _pedidosCompletos.value = emptyList()
                _pedidos.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    // ✅ MODIFICADO: Guardar en _pedidosCompletos
    fun cargarPedidosPasadoManana() {
        viewModelScope.launch {
            Log.d("PedidoViewModel", "📥 cargarPedidosPasadoManana()")
            _loading.value = true

            try {
                val lista = pedidoRepository.obtenerListaPedidosPasadoManana()

                Log.d("PedidoViewModel", "✅ Pedidos de PASADO MAÑANA recibidos: ${lista.size}")

                // ✅ CAMBIO: Guardar en ambas listas
                _pedidosCompletos.value = lista
                _pedidos.value = lista

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "❌ Error cargando pedidos de pasado mañana: ${e.message}", e)
                _pedidosCompletos.value = emptyList()
                _pedidos.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun cargarPedidos(forceRefresh: Boolean = false) {
        cargarPedidosHoy()
    }

    // ✅ CORREGIDO: Filtrar sobre _pedidosCompletos en lugar del repositorio
    fun filtrarPedidos(query: String) {
        Log.d("PedidoViewModel", "🔍 Filtrando con query: '$query'")

        val filtrados = if (query.isBlank()) {
            // Si el query está vacío, mostrar todos los pedidos completos
            Log.d("PedidoViewModel", "Query vacío, mostrando todos: ${_pedidosCompletos.value.size}")
            _pedidosCompletos.value
        } else {
            // Filtrar sobre la lista completa
            _pedidosCompletos.value.filter { pedido ->
                pedido.nombreCliente?.contains(query, ignoreCase = true) == true ||
                        pedido.direccion?.contains(query, ignoreCase = true) == true
            }.also { resultado ->
                Log.d("PedidoViewModel", "Resultados filtrados: ${resultado.size}")
            }
        }

        _pedidos.value = filtrados
    }

    fun marcarPedidoComoEntregado(pedidoId: Long) {
        viewModelScope.launch {
            try {
                _pedidoEntregadoState.value = EntregaState.Loading

                pedidoRepository.marcarComoEntregado(pedidoId)

                _pedidoEntregadoState.value = EntregaState.Success
                actualizarEfectivoDelDia()

                // Recargar la lista actual (hoy por defecto)
                cargarPedidosHoy()

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "Error al marcar entregado", e)
                _pedidoEntregadoState.value = EntregaState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun actualizarEfectivoDelDia() {
        viewModelScope.launch {
            _efectivoLoading.value = true

            try {
                val userId = tokenManager.getUserId() ?: 0L

                if (userId == 0L) {
                    Log.e("PedidoViewModel", "Usuario no autenticado")
                    _efectivoDelDia.value = 0.0
                    return@launch
                }

                val result = pedidoRepository.obtenerEfectivoDelDia(userId)

                result.onSuccess { efectivo ->
                    _efectivoDelDia.value = efectivo
                    Log.d("PedidoViewModel", "Efectivo del día actualizado: S/. $efectivo")
                }.onFailure { error ->
                    Log.e("PedidoViewModel", "Error al obtener efectivo: ${error.message}")
                    _efectivoDelDia.value = 0.0
                }

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "Excepción al actualizar efectivo", e)
                _efectivoDelDia.value = 0.0
            } finally {
                _efectivoLoading.value = false
            }
        }
    }

    fun eliminarPedido(pedidoId: Long) {
        viewModelScope.launch {
            try {
                _pedidoEliminadoState.value = EliminacionState.Loading

                val result = pedidoRepository.eliminarPedido(pedidoId)

                result.onSuccess {
                    Log.d("PedidoViewModel", "✅ Pedido $pedidoId eliminado exitosamente")

                    // ✅ SOLUCIÓN: Eliminar de las listas localmente sin recargar
                    val nuevaListaCompleta = _pedidosCompletos.value.filter { it.id != pedidoId }
                    val nuevaListaFiltrada = _pedidos.value.filter { it.id != pedidoId }

                    Log.d("PedidoViewModel", "📋 Antes: ${_pedidos.value.size}, Después: ${nuevaListaFiltrada.size}")

                    // Actualizar ambas listas
                    _pedidosCompletos.value = nuevaListaCompleta
                    _pedidos.value = nuevaListaFiltrada

                    _pedidoEliminadoState.value = EliminacionState.Success

                }.onFailure { error ->
                    Log.e("PedidoViewModel", "❌ Error al eliminar: ${error.message}")
                    _pedidoEliminadoState.value = EliminacionState.Error(
                        error.message ?: "Error desconocido"
                    )
                }

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "Error al eliminar pedido", e)
                _pedidoEliminadoState.value = EliminacionState.Error(
                    e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun resetearEstadoOrden() {
        _ordenGuardadoState.value = OrdenGuardadoState.Idle
    }

    fun resetearEstadoEliminacion() {
        _pedidoEliminadoState.value = EliminacionState.Idle
    }

    fun resetearEstadoEntrega() {
        _pedidoEntregadoState.value = EntregaState.Idle
    }
}
sealed class OrdenGuardadoState {
    object Idle : OrdenGuardadoState()
    object Loading : OrdenGuardadoState()
    object Success : OrdenGuardadoState()
    data class Error(val message: String) : OrdenGuardadoState()
}
sealed class EntregaState {
    object Idle : EntregaState()
    object Loading : EntregaState()
    object Success : EntregaState()
    data class Error(val message: String) : EntregaState()
}

sealed class EliminacionState {
    object Idle : EliminacionState()
    object Loading : EliminacionState()
    object Success : EliminacionState()
    data class Error(val message: String) : EliminacionState()
}