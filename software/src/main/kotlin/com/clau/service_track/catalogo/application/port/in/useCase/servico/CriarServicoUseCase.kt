package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.ValorMonetario

fun interface CriarServicoUseCase {
    fun executar(comando: CriarServicoCommand): Servico
}

data class CriarServicoCommand(
    val nome: String,
    val descricao: String,
    val valorReferencia: ValorMonetario?,
)
