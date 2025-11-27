package com.andresDev.puriapp.data.api

import com.andresDev.puriapp.data.model.Visita
import retrofit2.Response
import retrofit2.http.GET

interface VisitaApi {

    @GET("visitas")
    suspend fun obtenerVisitas(): Response<List<Visita>>
}