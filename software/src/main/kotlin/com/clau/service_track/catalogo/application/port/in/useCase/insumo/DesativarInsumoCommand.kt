package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.vo.DomainId

data class DesativarInsumoCommand(
    val id: DomainId,
)
