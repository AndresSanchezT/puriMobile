package com.andresDev.puriapp.data.api

import com.andresDev.puriapp.data.model.LoginRequest
import com.andresDev.puriapp.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}