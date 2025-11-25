package com.andresDev.puriapp.ui.pedidos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.model.Pedido
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

    private val _pedidos = MutableStateFlow<List<Pedido>>(emptyList())
    val pedidos = _pedidos.asStateFlow()

    private val _allPedidos = mutableListOf<Pedido>() // cache en memoria
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        cargarPedidos()
    }

    fun cargarPedidos() {
        viewModelScope.launch {
            Log.d("DEBUG", "Iniciando carga de pedidos...")
            _loading.value = true

            try {
                val lista = pedidoRepository.obtenerPedidos()

                Log.d("DEBUG", "Datos recibidos: $lista")
                Log.d("DEBUG", "Cantidad: ${lista.size}")

                _allPedidos.clear()
                _allPedidos.addAll(lista)
                _pedidos.value = lista

            } catch (e: Exception) {
                Log.e("DEBUG", "Error cargando pedidos: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }


    fun agregarPedido(pedido: Pedido) {
        _allPedidos.add(pedido)
        _pedidos.value = _allPedidos.toList()
    }

    fun actualizarPedido(pedido: Pedido) {
        val index = _allPedidos.indexOfFirst { it.id == pedido.id }
        if (index != -1) _allPedidos[index] = pedido
        _pedidos.value = _allPedidos.toList()
    }

    //AÑADIR FILTRO SI ESTA CANCELADO O NO
    fun filtrarPedidosCancelados(query: String, soloVisitados: Boolean = false) {
        val filtrados = _allPedidos.filter {
            it.cliente.nombreContacto.contains(query, ignoreCase = true)
        }
        _pedidos.value = filtrados
    }
}