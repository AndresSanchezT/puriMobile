package com.andresDev.puriapp.data.model


data class ReporteProductoDTO(

    val nombreProducto: String,


    val totalProductos: Double,


    val stockActual: Double,


    val stockMinimo: Double,


    val estado: String,

    val unidadMedida: String,


    val cantidadesPorPedido: String?
)