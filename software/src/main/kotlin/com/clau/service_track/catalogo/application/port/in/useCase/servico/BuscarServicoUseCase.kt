package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

import com.clau.service_track.catalogo.domain.model.Servico

fun interface BuscarServicoUseCase {
    fun executar(consulta: BuscarServicoQuery): Servico
}
