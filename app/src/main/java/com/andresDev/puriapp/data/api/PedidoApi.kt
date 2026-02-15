package com.andresDev.puriapp.data.api

import com.andresDev.puriapp.data.model.CambiarEstadoPedidoDTO
import com.andresDev.puriapp.data.model.OrdenPedidoRequest
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
import retrofit2.http.Query

interface PedidoApi {
    // ✅ ENDPOINTS DE HOY
    @GET("pedidos/all-mobile")
    suspend fun obtenerListaPedidosRegistradosHoy(): Response<List<PedidoListaReponse>>

    @GET("pedidos/all-mobile-admin")
    suspend fun obtenerListaPedidosTotalesHoy(): Response<List<PedidoListaReponse>>

    // ✅ ENDPOINTS DE MAÑANA
    @GET("pedidos/all-mobile/manana")
    suspend fun obtenerListaPedidosRegistradosManana(): Response<List<PedidoListaReponse>>

    @GET("pedidos/all-mobile-admin/manana")
    suspend fun obtenerListaPedidosTotalesManana(): Response<List<PedidoListaReponse>>

    // ✅ ENDPOINTS DE PASADO MAÑANA
    @GET("pedidos/all-mobile/pasado-manana")
    suspend fun obtenerListaPedidosRegistradosPasadoManana(): Response<List<PedidoListaReponse>>

    @GET("pedidos/all-mobile-admin/pasado-manana")
    suspend fun obtenerListaPedidosTotalesPasadoManana(): Response<List<PedidoListaReponse>>

    @GET("pedidos/{id}/completo")
    suspend fun obtenerPedidoPorId(@Path("id") id: Long): Response<Pedido>

    @POST("pedidos/registrar/{idCliente}/{idVendedor}")
    suspend fun registrarPedido(
        @Path("idCliente") idCliente: Long,
        @Path("idVendedor") idVendedor: Long,
        @Query("forzar") forzar: Boolean = false,
        @Query("tipoFecha") tipoFecha: String = "MANANA",
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

    @DELETE("pedidos/mobil/{id}")
    suspend fun eliminarPedido(@Path("id") id: Long): Response<Unit>

    @GET("pedidos/verificar/{id}")
    suspend fun verificarPedidoExistente(
        @Path("id") idCliente: Long,
        @Query("tipoFecha") tipoFecha: String
    ): Response<Boolean>

    @PUT("pedidos/actualizar-orden")
    suspend fun actualizarOrdenPedidos(
        @Body pedidos: List<OrdenPedidoRequest>
    ): Response<Unit>
}

data class EfectivoResponse(
    val success: Boolean,
    val idRepartidor: Long,
    val efectivo: Double
)