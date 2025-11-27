package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.VisitaApi
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.Visita
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitaRepository @Inject constructor(private val apiService: VisitaApi) {

    private var cacheVisitas: List<Visita> = emptyList()

    suspend fun obtenerVisitas(forceRefresh: Boolean = false): List<Visita> {
        if (cacheVisitas.isEmpty() || forceRefresh) {

            val response = apiService.obtenerVisitas()

            if (response.isSuccessful) {
                cacheVisitas = response.body() ?: emptyList()
            } else {
                // Manejo básico de error (no crashea)
                throw Exception("Error HTTP: ${response.code()}")
            }
        }

        return cacheVisitas
    }

    fun buscarEnCache(query: String): List<Visita> {
        return cacheVisitas.filter {
            it.cliente.nombreContacto.contains(query, ignoreCase = true)
        }
    }

    fun limpiarCache() {
        cacheVisitas = emptyList()
    }
}