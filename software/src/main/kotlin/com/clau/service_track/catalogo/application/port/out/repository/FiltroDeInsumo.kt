package com.clau.service_track.catalogo.application.port.out.repository

import com.clau.service_track.catalogo.domain.vo.DomainId

data class FiltroDeInsumo(
    val incluirInativos: Boolean = false,
    val categoriaId: DomainId? = null,
    val termo: String? = null,
)
