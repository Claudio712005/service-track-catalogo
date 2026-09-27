package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface DesativarInsumoUseCase {
    fun executar(comando: DesativarInsumoCommand)
}

data class DesativarInsumoCommand(
    val id: DomainId,
)
