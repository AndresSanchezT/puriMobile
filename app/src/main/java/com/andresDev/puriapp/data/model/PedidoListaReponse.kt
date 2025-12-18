package com.andresDev.puriapp.data.model

data class PedidoListaReponse(
        val id : Long,
        val estado: String,
        val tieneCredito:Boolean,
        val direccion: String,
        val nombreCliente: String,
        val total: Double
)
