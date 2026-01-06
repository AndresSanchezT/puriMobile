package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.ClienteApi
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoRequest
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

    suspend fun registrarCliente(cliente: Cliente): Result<Cliente> {
        return try {
            val response = apiService.registrarCliente(cliente)

            if (response.isSuccessful) {
                val clienteCreado = response.body()
                if (clienteCreado != null) {
                    // ✅ Agregar el nuevo cliente al caché existente
                    cacheClientes = cacheClientes + clienteCreado
                    cacheNormalizado = cacheClientes.map { cliente ->
                        cliente to cliente.nombreContacto.lowercase().trim()
                    }
                    Result.success(clienteCreado)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Datos inválidos. Verifica la información ingresada"
                    404 -> "Cliente o vendedor no encontrado"
                    409 -> "El cliente ya existe"
                    500 -> "Error del servidor. Intenta más tarde"
                    else -> "Error HTTP: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Sin conexión a internet"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Tiempo de espera agotado"))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
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

}
