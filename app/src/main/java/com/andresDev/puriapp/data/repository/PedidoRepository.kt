package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.PedidoApi
import com.andresDev.puriapp.data.model.Pedido
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

    fun buscarEnCache(query: String): List<Pedido> {
        return cachePedidos.filter {
            it.cliente.nombreContacto.contains(query, ignoreCase = true)
        }
    }

    fun limpiarCache() {
        cachePedidos = emptyList()
    }
}