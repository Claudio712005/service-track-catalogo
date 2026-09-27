package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.vo.DomainId

data class BuscarInsumoQuery(
    val id: DomainId? = null,
    val sku: String? = null,
)
