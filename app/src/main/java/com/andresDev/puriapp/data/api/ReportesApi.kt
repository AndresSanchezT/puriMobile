package com.andresDev.puriapp.data.api

import com.andresDev.puriapp.data.model.ReporteProductoDTO
import retrofit2.Response
import retrofit2.http.GET


interface ReportesApi{

    @GET("pedidos/productos-registrados/hoy")
    suspend fun getReporteProductosHoy(): Response<List<ReporteProductoDTO>>

    @GET("pedidos/productos-registrados/manana")
    suspend fun getReporteProductosManana(): Response<List<ReporteProductoDTO>>
}