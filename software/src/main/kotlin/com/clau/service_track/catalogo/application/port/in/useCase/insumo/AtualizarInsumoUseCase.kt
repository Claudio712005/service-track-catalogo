package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.model.Insumo

fun interface AtualizarInsumoUseCase {
    fun executar(comando: AtualizarInsumoCommand): Insumo
}
