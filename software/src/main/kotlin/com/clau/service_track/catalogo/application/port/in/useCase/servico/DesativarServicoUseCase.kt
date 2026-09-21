    package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface DesativarServicoUseCase {
    fun executar(comando: DesativarServicoCommand)
}

data class DesativarServicoCommand(
    val id: DomainId,
)
