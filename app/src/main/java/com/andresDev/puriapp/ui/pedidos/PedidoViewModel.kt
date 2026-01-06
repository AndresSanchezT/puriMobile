package com.andresDev.puriapp.ui.pedidos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.data.repository.PedidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PedidoViewModel @Inject constructor(private val pedidoRepository: PedidoRepository) :
    ViewModel() {

    private val _pedidos = MutableStateFlow<List<PedidoListaReponse>>(emptyList())
    val pedidos = _pedidos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _pedidoEntregadoState = MutableStateFlow<EntregaState>(EntregaState.Idle)
    val pedidoEntregadoState: StateFlow<EntregaState> = _pedidoEntregadoState.asStateFlow()

    init {
        cargarPedidos()
    }

    fun cargarPedidos() {
        viewModelScope.launch {
            Log.d("DEBUG", "Iniciando carga de pedidos...")
            _loading.value = true

            try {
                val lista = pedidoRepository.obtenerListaPedidos()

                Log.d("DEBUG", "Datos recibidos: $lista")
                Log.d("DEBUG", "Cantidad: ${lista.size}")

                _pedidos.value = lista

            } catch (e: Exception) {
                Log.e("DEBUG", "Error cargando pedidos: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }


//    fun agregarPedido(pedido: Pedido) {
//        viewModelScope.launch {
//            try {
//                val nuevoPedido = pedidoRepository.crearPedido(pedido)
//                cargarPedidos() // Recargar para actualizar UI
//            } catch (e: Exception) {
//                Log.e("PedidoViewModel", "Error al agregar: ${e.message}")
//            }
//        }
//    }


//    fun actualizarPedido(pedido: Pedido) {
//        val index = _allPedidos.indexOfFirst { it.id == pedido.id }
//        if (index != -1) _allPedidos[index] = pedido
//        _pedidos.value = _allPedidos.toList()
//    }

    //AÑADIR FILTRO SI ESTA CANCELADO O NO
    fun filtrarPedidos(query: String) {
        viewModelScope.launch {
            val filtrados = pedidoRepository.buscarEnCache(query)
            _pedidos.value = filtrados
        }
    }

    fun marcarPedidoComoEntregado(pedidoId: Long) {
        viewModelScope.launch {
            try {
                _pedidoEntregadoState.value = EntregaState.Loading

                pedidoRepository.marcarComoEntregado(pedidoId)

                _pedidoEntregadoState.value = EntregaState.Success

                cargarPedidos() // recargar lista

            } catch (e: Exception) {
                Log.e("PedidoViewModel", "Error al marcar entregado", e)
                _pedidoEntregadoState.value = EntregaState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // ✅ AGREGAR: Método para resetear el estado
    fun resetearEstadoEntrega() {
        _pedidoEntregadoState.value = EntregaState.Idle
    }
}
sealed class EntregaState {
    object Idle : EntregaState()
    object Loading : EntregaState()
    object Success : EntregaState()
    data class Error(val message: String) : EntregaState()
}