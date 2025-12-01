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

    // Cache normalizado para búsqueda más rápida
    private var cacheNormalizado: List<Pair<Cliente, String>> = emptyList()

    suspend fun obtenerClientes(): List<Cliente> {
        if (cacheClientes.isEmpty()) {
            cacheClientes = apiService.obtenerClientes()
            // Normalizar al cargar para búsquedas más rápidas
            cacheNormalizado = cacheClientes.map { cliente ->
                cliente to cliente.nombreContacto.lowercase().trim()
            }
        }
        return cacheClientes
    }
    // Búsqueda optimizada con texto normalizado
    fun buscarEnCache(query: String): List<Cliente> {
        if (query.isEmpty()) return cacheClientes

        val queryNormalizado = query.lowercase().trim()

        return cacheNormalizado
            .filter { (_, nombreNormalizado) ->
                nombreNormalizado.contains(queryNormalizado)
            }
            .map { (cliente, _) -> cliente }
    }

    suspend fun refrescarCache(): List<Cliente> {
        cacheClientes = apiService.obtenerClientes()
        cacheNormalizado = cacheClientes.map { cliente ->
            cliente to cliente.nombreContacto.lowercase().trim()
        }
        return cacheClientes
    }


    fun limpiarCache() {
        cacheClientes = emptyList()
        cacheNormalizado = emptyList()
    }

    suspend fun crearCliente(cliente: Cliente){

    }


}
