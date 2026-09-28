package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.model.Insumo

fun interface BuscarInsumoUseCase {
    fun executar(consulta: BuscarInsumoQuery): Insumo
}
