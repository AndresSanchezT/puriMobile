package com.andresDev.puriapp.data.model

data class CambiarEstadoPedidoDTO(
    val nuevoEstado: String,
    val motivoAnulacion: String?,
    val idRepartidor: Long
)
