package com.andresDev.puriapp.data.api

import com.andresDev.puriapp.data.model.Pedido
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PedidoApi {
    @GET("pedidos")
    suspend fun obtenerPedidos(): Response<List<Pedido>>  // ← Devuelve Response

    @GET("pedidos/{id}")
    suspend fun obtenerPedidoPorId(@Path("id") id: Long): Response<Pedido>

    @POST("pedidos")
    suspend fun crearPedido(@Body pedido: Pedido): Response<Pedido>

    @PUT("pedidos/{id}")
    suspend fun actualizarPedido(
        @Path("id") id: Long,
        @Body pedido: Pedido
    ): Response<Pedido>

    @DELETE("pedidos/{id}")
    suspend fun eliminarPedido(@Path("id") id: Long): Response<Unit>
}