package com.andresDev.puriapp.data.repository

import android.util.Log
import com.andresDev.puriapp.data.api.PedidoApi
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.model.CambiarEstadoPedidoDTO
import com.andresDev.puriapp.data.model.OrdenPedidoRequest
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.data.model.PedidoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedidoRepository @Inject constructor(
    private val apiService: PedidoApi,
    private val tokenManager: TokenManager
) {
    private var cachePedidos: List<PedidoListaReponse> = emptyList()
    private var ultimoRolUsado: String? = null

    private val isAdmin: Boolean
        get() = tokenManager.getUserRole()?.equals("administrador", ignoreCase = true) == true

    // ✅ NUEVO: Obtener pedidos de HOY
    suspend fun obtenerListaPedidosHoy(): List<PedidoListaReponse> {
        Log.d("PedidoRepository", "🔍 obtenerListaPedidosHoy()")
        Log.d("PedidoRepository", "  - isAdmin: $isAdmin")

        val response = if (isAdmin) {
            Log.d("PedidoRepository", "📡 Endpoint ADMIN: /pedidos/all-mobile-admin")
            apiService.obtenerListaPedidosTotalesHoy()
        } else {
            Log.d("PedidoRepository", "📡 Endpoint REPARTIDOR: /pedidos/all-mobile")
            apiService.obtenerListaPedidosRegistradosHoy()
        }

        if (response.isSuccessful) {
            val body = response.body()
            Log.d("PedidoRepository", "✅ Response exitoso - Pedidos HOY: ${body?.size}")
            return body ?: emptyList()
        } else {
            Log.e("PedidoRepository", "❌ Error HTTP: ${response.code()}")
            throw Exception("Error HTTP: ${response.code()}")
        }
    }

    // ✅ NUEVO: Obtener pedidos de MAÑANA
    suspend fun obtenerListaPedidosManana(): List<PedidoListaReponse> {
        Log.d("PedidoRepository", "🔍 obtenerListaPedidosManana()")
        Log.d("PedidoRepository", "  - isAdmin: $isAdmin")

        val response = if (isAdmin) {
            Log.d("PedidoRepository", "📡 Endpoint ADMIN: /pedidos/all-mobile-admin/manana")
            apiService.obtenerListaPedidosTotalesManana()
        } else {
            Log.d("PedidoRepository", "📡 Endpoint REPARTIDOR: /pedidos/all-mobile/manana")
            apiService.obtenerListaPedidosRegistradosManana()
        }

        if (response.isSuccessful) {
            val body = response.body()
            Log.d("PedidoRepository", "✅ Response exitoso - Pedidos MAÑANA: ${body?.size}")
            return body ?: emptyList()
        } else {
            Log.e("PedidoRepository", "❌ Error HTTP: ${response.code()}")
            throw Exception("Error HTTP: ${response.code()}")
        }
    }

    // ✅ NUEVO: Obtener pedidos de PASADO MAÑANA
    suspend fun obtenerListaPedidosPasadoManana(): List<PedidoListaReponse> {
        Log.d("PedidoRepository", "🔍 obtenerListaPedidosPasadoManana()")
        Log.d("PedidoRepository", "  - isAdmin: $isAdmin")

        val response = if (isAdmin) {
            Log.d("PedidoRepository", "📡 Endpoint ADMIN: /pedidos/all-mobile-admin/pasado-manana")
            apiService.obtenerListaPedidosTotalesPasadoManana()
        } else {
            Log.d("PedidoRepository", "📡 Endpoint REPARTIDOR: /pedidos/all-mobile/pasado-manana")
            apiService.obtenerListaPedidosRegistradosPasadoManana()
        }

        if (response.isSuccessful) {
            val body = response.body()
            Log.d("PedidoRepository", "✅ Response exitoso - Pedidos PASADO MAÑANA: ${body?.size}")
            return body ?: emptyList()
        } else {
            Log.e("PedidoRepository", "❌ Error HTTP: ${response.code()}")
            throw Exception("Error HTTP: ${response.code()}")
        }
    }

    // 🔄 MANTENER para compatibilidad (ahora llama a obtenerListaPedidosHoy)
    suspend fun obtenerListaPedidos(forceRefresh: Boolean = false): List<PedidoListaReponse> {
        val rolActual = tokenManager.getUserRole()
        val rolCambio = ultimoRolUsado != null && ultimoRolUsado != rolActual

        Log.d("PedidoRepository", "🔍 obtenerListaPedidos()")
        Log.d("PedidoRepository", "  - forceRefresh: $forceRefresh")
        Log.d("PedidoRepository", "  - cachePedidos.size: ${cachePedidos.size}")
        Log.d("PedidoRepository", "  - rolActual: $rolActual")
        Log.d("PedidoRepository", "  - rolCambio: $rolCambio")
        Log.d("PedidoRepository", "  - isAdmin: $isAdmin")

        if (cachePedidos.isEmpty() || forceRefresh || rolCambio) {
            Log.d("PedidoRepository", "🌐 Llamando al API...")

            ultimoRolUsado = rolActual

            val response = if (isAdmin) {
                Log.d("PedidoRepository", "📡 Endpoint ADMIN: /pedidos/all-mobile-admin")
                apiService.obtenerListaPedidosTotalesHoy()
            } else {
                Log.d("PedidoRepository", "📡 Endpoint REPARTIDOR: /pedidos/all-mobile")
                apiService.obtenerListaPedidosRegistradosHoy()
            }

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("PedidoRepository", "✅ Response exitoso")
                Log.d("PedidoRepository", "  - body?.size: ${body?.size}")

                cachePedidos = body ?: emptyList()

                Log.d("PedidoRepository", "💾 Cache actualizado con ${cachePedidos.size} pedidos")
            } else {
                Log.e("PedidoRepository", "❌ Error HTTP: ${response.code()}")
                Log.e("PedidoRepository", "  - message: ${response.message()}")
                Log.e("PedidoRepository", "  - errorBody: ${response.errorBody()?.string()}")
                throw Exception("Error HTTP: ${response.code()}")
            }
        } else {
            Log.d("PedidoRepository", "📦 Usando cache (${cachePedidos.size} pedidos)")
        }

        return cachePedidos
    }

    suspend fun marcarComoEntregado(pedidoId: Long) {
        val idRepartidor = tokenManager.getUserId()
        if (idRepartidor == 0L) {
            throw Exception("Usuario no autenticado")
        }

        val dto = CambiarEstadoPedidoDTO(
            nuevoEstado = "entregado",
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
        pedidoRequest: PedidoRequest,
        forzar : Boolean,
        tipoFecha: String
    ): Result<PedidoListaReponse> {
        return try {
            val response = apiService.registrarPedido(idCliente, idVendedor, forzar, tipoFecha, pedidoRequest)

            if (response.isSuccessful) {
                val pedidoCreado = response.body()
                if (pedidoCreado != null) {
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

                    cachePedidos = cachePedidos.filter { it.id != pedidoAResponse.id } + pedidoAResponse

                    Result.success(pedidoActualizado)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Datos inválidos del pedido"
                    404 -> "Pedido no encontrado"
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
        val filtrados = cachePedidos.filter {
            it.nombreCliente.contains(query, ignoreCase = true)
        }

        return if (!isAdmin) {
            filtrados.filter { it.estado?.equals("registrado", ignoreCase = true) == true }
        } else {
            filtrados
        }
    }
    suspend fun verificarPedidoExistente(
        idCliente: Long,
        tipoFecha: String
    ): Result<Boolean> {
        return try {
            val response = apiService.verificarPedidoExistente(idCliente, tipoFecha)

            if (response.isSuccessful) {
                Result.success(response.body() ?: false)
            } else {
                Result.failure(Exception("Error al verificar pedido: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarPedido(pedidoId: Long): Result<Unit> {
        return try {
            val response = apiService.eliminarPedido(pedidoId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("PedidoRepository", "Error eliminando pedido", e)
            Result.failure(e)
        }
    }

    suspend fun actualizarOrdenPedidos(pedidos: List<PedidoListaReponse>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ Convertir a DTO
                val ordenRequests = pedidos.mapIndexed { index, pedido ->
                    OrdenPedidoRequest(
                        id = pedido.id ?: 0L,
                        orden = index
                    )
                }

                Log.d("PedidoRepository", "📤 Enviando orden al servidor: $ordenRequests")

                val response = apiService.actualizarOrdenPedidos(ordenRequests)

                if (response.isSuccessful) {
                    Log.d("PedidoRepository", "✅ Servidor respondió OK")
                    Result.success(Unit)
                } else {
                    Log.e("PedidoRepository", "❌ Error del servidor: ${response.code()}")
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("PedidoRepository", "❌ Excepción en actualizarOrdenPedidos", e)
                Result.failure(e)
            }
        }
    }

    fun limpiarCache() {
        Log.d("PedidoRepository", "🗑️ Limpiando cache...")
        cachePedidos = emptyList()
        ultimoRolUsado = null
    }
}