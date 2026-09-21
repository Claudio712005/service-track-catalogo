package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface BuscarServicoUseCase {
    fun executar(consulta: BuscarServicoQuery): Servico
}

data class BuscarServicoQuery(
    val id: DomainId,
)
