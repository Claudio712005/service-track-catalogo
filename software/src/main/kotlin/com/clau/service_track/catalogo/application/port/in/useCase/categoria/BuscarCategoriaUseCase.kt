package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface BuscarCategoriaUseCase {
    fun executar(consulta: BuscarCategoriaQuery): CategoriaDeInsumo
}

data class BuscarCategoriaQuery(
    val id: DomainId,
)
