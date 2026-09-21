package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.model.Servico

fun interface ListarServicosUseCase {
    fun executar(consulta: ListarServicosQuery): List<Servico>
}

data class ListarServicosQuery(
    val incluirInativos: Boolean = false,
)
