package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface ConsumirReservaUseCase {
    fun executar(comando: ConsumirReservaCommand): ResultadoDoPasso
}

data class ConsumirReservaCommand(
    val insumoId: DomainId,
    val ordemServicoId: DomainId,
    val chaveIdempotencia: String,
    val traceId: String?,
)
