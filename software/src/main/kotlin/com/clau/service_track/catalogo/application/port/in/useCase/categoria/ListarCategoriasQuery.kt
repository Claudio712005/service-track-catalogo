package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

data class ListarCategoriasQuery(
    val termo: String? = null,
    val incluirDesativadas: Boolean = false,
)
