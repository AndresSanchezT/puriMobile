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

    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes = _clientes.asStateFlow()

    private val _allClientes = mutableListOf<Cliente>() // cache en memoria
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

                _allClientes.clear()
                _allClientes.addAll(lista)
                _clientes.value = lista

            } catch (e: Exception) {
                Log.e("DEBUG", "Error cargando clientes: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }


    fun agregarCliente(cliente: Cliente) {
        _allClientes.add(cliente)
        _clientes.value = _allClientes.toList()
    }

    fun actualizarCliente(cliente: Cliente) {
        val index = _allClientes.indexOfFirst { it.id == cliente.id }
        if (index != -1) _allClientes[index] = cliente
        _clientes.value = _allClientes.toList()
    }

    //AÑADIR FILTRO SI ESTA VISITADO O NO
    fun filtrarClientes(query: String, soloVisitados: Boolean = false) {
        val filtrados = _allClientes.filter {
            it.nombreContacto.contains(query, ignoreCase = true)
        }
        _clientes.value = filtrados
    }
}