package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface ConsultarSaldoUseCase {
    fun executar(consulta: ConsultarSaldoQuery): ResultadoDeSaldo
}

data class ConsultarSaldoQuery(
    val insumoId: DomainId,
)
