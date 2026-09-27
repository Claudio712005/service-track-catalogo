package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo

fun interface ListarCategoriasUseCase {
    fun executar(consulta: ListarCategoriasQuery): List<CategoriaDeInsumo>
}

data class ListarCategoriasQuery(
    val termo: String? = null,
)
