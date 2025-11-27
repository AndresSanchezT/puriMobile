package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.ClienteApi
import com.andresDev.puriapp.data.model.Cliente
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClienteRepository @Inject constructor(
    private val apiService: ClienteApi
) {

    // Cache en memoria
    private var cacheClientes: List<Cliente> = emptyList()

    suspend fun obtenerClientes(): List<Cliente> {
        if (cacheClientes.isEmpty()) {
            // Petición HTTP solo si cache está vacía
            cacheClientes = apiService.obtenerClientes()
        }
        return cacheClientes
    }

    fun buscarEnCache(query: String): List<Cliente> {
        return cacheClientes.filter {
            it.nombreContacto.contains(query, ignoreCase = true)
        }
    }

    suspend fun crearCliente(cliente: Cliente){

    }
}
