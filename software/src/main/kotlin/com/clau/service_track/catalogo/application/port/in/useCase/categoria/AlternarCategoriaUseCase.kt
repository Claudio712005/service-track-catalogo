package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface AlternarCategoriaUseCase {
    fun executar(comando: AlternarCategoriaCommand): CategoriaDeInsumo
}

data class AlternarCategoriaCommand(
    val id: DomainId,
    val ativa: Boolean,
)
