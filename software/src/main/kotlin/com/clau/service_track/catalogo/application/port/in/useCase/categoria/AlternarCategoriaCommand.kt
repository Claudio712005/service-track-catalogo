package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.vo.DomainId

data class AlternarCategoriaCommand(
    val id: DomainId,
    val ativa: Boolean,
)
