package com.andresDev.puriapp.data.api


import com.andresDev.puriapp.data.model.CambiarEstadoPedidoDTO
import com.andresDev.puriapp.data.model.Pedido
import com.andresDev.puriapp.data.model.PedidoListaReponse
import com.andresDev.puriapp.data.model.PedidoRequest
import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


interface PedidoApi {
    @GET("pedidos/all-mobile")
    suspend fun obtenerListaPedidos(): Response<List<PedidoListaReponse>>  // ← Devuelve Response

    @GET("pedidos/{id}/completo")
    suspend fun obtenerPedidoPorId(@Path("id") id: Long): Response<Pedido>

    @POST("pedidos/registrar/{idCliente}/{idVendedor}")
    suspend fun registrarPedido(
        @Path("idCliente") idCliente: Long,
        @Path("idVendedor") idVendedor: Long,
        @Body pedidoRequest: PedidoRequest
    ): Response<PedidoListaReponse>


    @PUT("pedidos/{id}")
    suspend fun actualizarPedido(
        @Path("id") id: Long,
        @Body pedido: Pedido
    ): Response<Pedido>

    @PATCH("pedidos/{id}/estado-movil")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Body dto: CambiarEstadoPedidoDTO
    ): Response<ResponseBody>

    @GET("pedidos/efectivo-del-dia/{idRepartidor}")
    suspend fun obtenerEfectivoDelDia(
        @Path("idRepartidor") idRepartidor: Long
    ): Response<EfectivoResponse>

    @DELETE("pedidos/{id}")
    suspend fun eliminarPedido(@Path("id") id: Long): Response<Unit>
}
data class EfectivoResponse(
    val success: Boolean,
    val idRepartidor: Long,
    val efectivo: Double
)
