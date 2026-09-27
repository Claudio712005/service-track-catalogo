package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.model.Servico

fun interface AtualizarServicoUseCase {
    fun executar(comando: AtualizarServicoCommand): Servico
}
