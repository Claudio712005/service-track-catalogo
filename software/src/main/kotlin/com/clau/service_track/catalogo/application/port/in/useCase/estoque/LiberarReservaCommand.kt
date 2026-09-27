package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.vo.DomainId

data class LiberarReservaCommand(
    val insumoId: DomainId,
    val ordemServicoId: DomainId,
    val chaveIdempotencia: String,
    val traceId: String?,
)
