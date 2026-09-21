package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario

fun interface AtualizarServicoUseCase {
    fun executar(comando: AtualizarServicoCommand): Servico
}

data class AtualizarServicoCommand(
    val id: DomainId,
    val descricao: String,
    val valorReferencia: ValorMonetario?,
)
