package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.PedidoApi
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.data.model.PedidoRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton  // ← ✅ IMPORTANTE para mantener el caché
class PedidoRepository @Inject constructor(
    private val apiService: PedidoApi
) {
    private var cachePedidos: List<PedidoListaReponse> = emptyList()

    suspend fun obtenerListaPedidos(forceRefresh: Boolean = false): List<PedidoListaReponse> {
        if (cachePedidos.isEmpty() || forceRefresh) {

            val response = apiService.obtenerListaPedidos()

            if (response.isSuccessful) {
                cachePedidos = response.body() ?: emptyList()
            } else {
                // Manejo básico de error (no crashea)
                throw Exception("Error HTTP: ${response.code()}")
            }
        }

        return cachePedidos
    }

    suspend fun obtenerPedidoPorId(id: Long): Pedido {
        val response = apiService.obtenerPedidoPorId(id)

        if (!response.isSuccessful) {
            throw Exception("Error HTTP: ${response.code()}")
        }

        return response.body() ?: throw Exception("Body vacío en la respuesta")
    }
    suspend fun registrarPedido(
        idCliente: Long,
        idVendedor: Long,
        pedidoRequest: PedidoRequest
    ): Result<Pedido> {
        return try {
            val response = apiService.registrarPedido(idCliente, idVendedor, pedidoRequest)

            if (response.isSuccessful) {
                val pedido = response.body()
                if (pedido != null) {
                    // Limpiar cache para forzar recarga
                    limpiarCache()
                    Result.success(pedido)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Datos inválidos"
                    404 -> "Cliente o vendedor no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error HTTP: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    fun buscarEnCache(query: String): List<PedidoListaReponse> {
        return cachePedidos.filter {
            it.nombreCliente.contains(query, ignoreCase = true)
        }
    }

    fun limpiarCache() {
        cachePedidos = emptyList()
    }
}