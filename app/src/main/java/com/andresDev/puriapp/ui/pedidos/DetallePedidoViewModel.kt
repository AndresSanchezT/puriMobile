package com.andresDev.puriapp.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.repository.PedidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetallePedidoViewModel @Inject constructor(
    private val pedidoRepository: PedidoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetallePedidoUiState>(DetallePedidoUiState.Loading)
    val uiState: StateFlow<DetallePedidoUiState> = _uiState.asStateFlow()

    fun cargarPedido(pedidoId: Long) {
        viewModelScope.launch {
            _uiState.value = DetallePedidoUiState.Loading

            try {
                val pedido = pedidoRepository.obtenerPedidoPorId(pedidoId)
                _uiState.value = DetallePedidoUiState.Success(pedido)

            } catch (e: Exception) {
                _uiState.value = DetallePedidoUiState.Error(
                    e.message ?: "Error al cargar pedido"
                )
            }
        }
    }
}

// Sealed class para estados
sealed class DetallePedidoUiState {
    object Loading : DetallePedidoUiState()
    data class Success(val pedido: Pedido) : DetallePedidoUiState()
    data class Error(val message: String) : DetallePedidoUiState()
}