package com.andresDev.puriapp.ui.clientes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.repository.ClienteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClienteViewModel @Inject constructor(
    private val repository: ClienteRepository
) : ViewModel() {

    private val _registroClienteState = MutableStateFlow<RegistroClienteState>(RegistroClienteState.Idle)
    val registroClienteState: StateFlow<RegistroClienteState> = _registroClienteState.asStateFlow()

    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes = _clientes.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        cargarClientes()
    }

    fun cargarClientes() {
        viewModelScope.launch {
            Log.d("DEBUG", "Iniciando carga de clientes...")
            _loading.value = true

            try {
                val lista = repository.obtenerClientes()

                Log.d("DEBUG", "Datos recibidos: $lista")
                Log.d("DEBUG", "Cantidad: ${lista.size}")
                _clientes.value = lista

            } catch (e: Exception) {
                Log.e("DEBUG", "Error cargando clientes: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }


    // ⚠️ Estos métodos NO actualizan la API, solo la memoria
    fun registrarCliente(cliente: Cliente) {
        viewModelScope.launch {
            _registroClienteState.value = RegistroClienteState.Loading

            val result = repository.registrarCliente(cliente)

            result.fold(
                onSuccess = { cliente ->
                    _registroClienteState.value = RegistroClienteState.Success(cliente)
                    // Refrescar la lista de clientes después del registro
                    cargarClientes()
                },
                onFailure = { error ->
                    _registroClienteState.value = RegistroClienteState.Error(
                        error.message ?: "Error desconocido"
                    )
                }
            )
        }
    }

//    fun actualizarCliente(cliente: Cliente) {
//        val index = _allClientes.indexOfFirst { it.id == cliente.id }
//        if (index != -1) _allClientes[index] = cliente
//        _clientes.value = _allClientes.toList()
//    }

    //AÑADIR FILTRO SI ESTA VISITADO O NO
    fun filtrarClientes(query: String) {
        viewModelScope.launch {
            val filtrados = repository.buscarEnCache(query)
            _clientes.value = filtrados
        }
    }
}