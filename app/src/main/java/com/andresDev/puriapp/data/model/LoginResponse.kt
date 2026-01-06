package com.andresDev.puriapp.data.model

data class LoginResponse(
    val token: String,
    val id: Long,
    val username: String,
    val role: String,
    val nombre: String
)
