package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.vo.DomainId

data class BuscarServicoQuery(
    val id: DomainId,
)
