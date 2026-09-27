package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.vo.DomainId

data class ListarInsumosQuery(
    val incluirInativos: Boolean = false,
    val categoriaId: DomainId? = null,
    val termo: String? = null,
)
