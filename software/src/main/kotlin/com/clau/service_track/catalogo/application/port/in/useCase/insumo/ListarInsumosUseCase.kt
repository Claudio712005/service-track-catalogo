package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface ListarInsumosUseCase {
    fun executar(consulta: ListarInsumosQuery): List<Insumo>
}

data class ListarInsumosQuery(
    val incluirInativos: Boolean = false,
    val categoriaId: DomainId? = null,
    val termo: String? = null,
)
