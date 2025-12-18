package com.andresDev.puriapp.data.api

import com.andresDev.puriapp.data.model.Cliente
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface ClienteApi {
    @GET("clientes")
    suspend fun obtenerClientes(): List<Cliente>

    @POST("clientes")
    suspend fun registrarCliente(@Body cliente: Cliente): Response<Cliente>
}