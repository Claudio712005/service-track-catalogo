package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.model.Servico

fun interface CriarServicoUseCase {
    fun executar(comando: CriarServicoCommand): Servico
}
