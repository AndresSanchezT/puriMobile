package com.andresDev.puriapp.data.api


import com.andresDev.puriapp.data.model.Producto
import retrofit2.Response
import retrofit2.http.GET

interface ProductoApi {
    @GET("productos/all")
    suspend fun obtenerProductos(): Response<List<Producto>>
}