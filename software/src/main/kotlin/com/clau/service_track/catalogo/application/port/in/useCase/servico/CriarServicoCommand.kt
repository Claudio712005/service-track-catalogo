package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.vo.ValorMonetario

data class CriarServicoCommand(
    val nome: String,
    val descricao: String,
    val valorReferencia: ValorMonetario?,
)
