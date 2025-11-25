package com.andresDev.puriapp.data.api

import com.andresDev.puriapp.data.model.Cliente
import retrofit2.http.GET

interface ClienteApi {
    @GET("clientes")
    suspend fun obtenerClientes(): List<Cliente>
}