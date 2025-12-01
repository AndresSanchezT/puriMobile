package com.andresDev.puriapp.data.repository

import com.andresDev.puriapp.data.api.ProductoApi
import com.andresDev.puriapp.data.model.Producto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoRepository @Inject constructor(private val productoApi: ProductoApi) {

    // Cache en memoria
    private var cacheProductos: List<Producto> = emptyList()

    // Cache normalizado para búsqueda más rápida
    private var cacheNormalizado: List<Pair<Producto, String>> = emptyList()

    suspend fun obtenerProductos(): List<Producto> {
        if (cacheProductos.isEmpty()) {
            val response = productoApi.obtenerProductos()
            if (response.isSuccessful) {
                cacheProductos = response.body() ?: emptyList()
                // Normalizar para búsquedas más rápidas
                cacheNormalizado = cacheProductos.map { producto ->
                    producto to "${producto.nombre} ${producto.codigo}".lowercase().trim()
                }
            } else {
                throw Exception("Error al obtener productos: ${response.message()}")
            }
        }
        return cacheProductos
    }

    // Búsqueda optimizada
    fun buscarEnCache(query: String): List<Producto> {
        if (query.isEmpty()) return cacheProductos

        val queryNormalizado = query.lowercase().trim()

        return cacheNormalizado
            .filter { (_, textoNormalizado) ->
                textoNormalizado.contains(queryNormalizado)
            }
            .map { (producto, _) -> producto }
    }

    suspend fun refrescarCache(): List<Producto> {
        val response = productoApi.obtenerProductos()
        if (response.isSuccessful) {
            cacheProductos = response.body() ?: emptyList()
            cacheNormalizado = cacheProductos.map { producto ->
                producto to "${producto.nombre} ${producto.codigo}".lowercase().trim()
            }
        }
        return cacheProductos
    }

    fun limpiarCache() {
        cacheProductos = emptyList()
        cacheNormalizado = emptyList()
    }
}