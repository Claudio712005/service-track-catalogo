package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface BuscarInsumoUseCase {
    fun executar(consulta: BuscarInsumoQuery): Insumo
}

data class BuscarInsumoQuery(
    val id: DomainId? = null,
    val sku: String? = null,
)
