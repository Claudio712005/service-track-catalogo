package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario

data class AtualizarServicoCommand(
    val id: DomainId,
    val descricao: String,
    val valorReferencia: ValorMonetario?,
)
