package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.PedidoApi
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.model.CambiarEstadoPedidoDTO
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoDetallesGeneralesResponse
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.data.model.PedidoRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Long
import kotlin.collections.plus

@Singleton  // ← ✅ IMPORTANTE para mantener el caché
class PedidoRepository @Inject constructor(
    private val apiService: PedidoApi,
    private val tokenManager: TokenManager
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

    suspend fun marcarComoEntregado(pedidoId: Long) {

        val idRepartidor = tokenManager.getUserId()
        if (idRepartidor == 0L) {
            throw Exception("Usuario no autenticado")
        }

        val dto = CambiarEstadoPedidoDTO(
            nuevoEstado = "entregado", // usa mayúsculas si tu backend usa enum
            motivoAnulacion = null,
            idRepartidor = idRepartidor
        )

        val response = apiService.cambiarEstado(pedidoId, dto)

        if (!response.isSuccessful) {
            throw Exception("Error HTTP ${response.code()}")
        }

        limpiarCache()
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
    ): Result<PedidoListaReponse> {
        return try {
            val response = apiService.registrarPedido(idCliente, idVendedor, pedidoRequest)

            if (response.isSuccessful) {
                val pedidoCreado = response.body()
                if (pedidoCreado != null) {
                    // ✅ Agregar el nuevo cliente al caché existente
                    cachePedidos = cachePedidos + pedidoCreado
                    Result.success(pedidoCreado)
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

    suspend fun actualizarPedido(id: Long, pedido: Pedido): Result<Pedido> {
        return try {
            val response = apiService.actualizarPedido(id, pedido)

            if (response.isSuccessful) {
                val pedidoActualizado = response.body()
                if (pedidoActualizado != null) {
                    // Validar que tenga los datos necesarios
                    if (pedidoActualizado.id == null ||
                        pedidoActualizado.estado == null ||
                        pedidoActualizado.cliente == null ||
                        pedidoActualizado.total == null) {
                        return Result.failure(Exception("Datos incompletos del servidor"))
                    }

                    val pedidoAResponse = PedidoListaReponse(
                        id = pedidoActualizado.id,
                        estado = pedidoActualizado.estado,
                        tieneCredito = pedidoActualizado.cliente.tieneCredito,
                        direccion = pedidoActualizado.cliente.direccion,
                        nombreCliente = pedidoActualizado.cliente.nombreContacto,
                        total = pedidoActualizado.total
                    )

                    // Actualizar caché: reemplazar si existe, agregar si no
                    cachePedidos = cachePedidos.filter { it.id != pedidoAResponse.id } + pedidoAResponse

                    Result.success(pedidoActualizado)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Datos inválidos del pedido"
                    404 -> "Pedido no encontrado"  // ✅ Corregido
                    500 -> "Error del servidor"
                    else -> "Error HTTP: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    suspend fun obtenerEfectivoDelDia(idRepartidor: Long): Result<Double> {
        return try {
            val response = apiService.obtenerEfectivoDelDia(idRepartidor)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.efectivo)
                } else {
                    Result.failure(Exception("Respuesta inválida del servidor"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Repartidor no encontrado"
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