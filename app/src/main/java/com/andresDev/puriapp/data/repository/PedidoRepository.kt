package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.PedidoApi
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton  // ← ✅ IMPORTANTE para mantener el caché
class PedidoRepository @Inject constructor(
    private val apiService: PedidoApi
) {
    private var cachePedidos: List<Pedido> = emptyList()

    suspend fun obtenerPedidos(forceRefresh: Boolean = false): List<Pedido> {
        if (cachePedidos.isEmpty() || forceRefresh) {

            val response = apiService.obtenerPedidos()

            if (response.isSuccessful) {
                cachePedidos = response.body() ?: emptyList()
            } else {
                // Manejo básico de error (no crashea)
                throw Exception("Error HTTP: ${response.code()}")
            }
        }

        return cachePedidos
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

    fun buscarEnCache(query: String): List<Pedido> {
        return cachePedidos.filter {
            it.cliente.nombreContacto.contains(query, ignoreCase = true)
        }
    }

    fun limpiarCache() {
        cachePedidos = emptyList()
    }
}