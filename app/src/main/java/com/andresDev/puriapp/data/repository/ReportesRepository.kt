package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.ReportesApi
import com.andresDev.puriapp.data.manager.TokenManager
import com.andresDev.puriapp.data.model.ReporteProductoDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportesRepository @Inject constructor(
    private val apiService: ReportesApi,
    private val tokenManager: TokenManager
) {

    /**
     * Obtiene el reporte de productos registrados HOY
     * Siempre trae datos frescos del servidor
     */
    suspend fun obtenerReporteHoy(): Result<List<ReporteProductoDTO>> {
        return try {
            val response = apiService.getReporteProductosHoy()

            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Result.success(data)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Sesión expirada. Inicia sesión nuevamente"
                    403 -> "No tienes permisos para ver este reporte"
                    404 -> "Reporte no encontrado"
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
            Result.failure(Exception("Error al cargar reporte: ${e.message}"))
        }
    }

    /**
     * Obtiene el reporte de productos registrados MAÑANA
     * Siempre trae datos frescos del servidor
     */
    suspend fun obtenerReporteManana(): Result<List<ReporteProductoDTO>> {
        return try {
            val response = apiService.getReporteProductosManana()

            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Result.success(data)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Sesión expirada. Inicia sesión nuevamente"
                    403 -> "No tienes permisos para ver este reporte"
                    404 -> "Reporte no encontrado"
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
            Result.failure(Exception("Error al cargar reporte: ${e.message}"))
        }
    }
}