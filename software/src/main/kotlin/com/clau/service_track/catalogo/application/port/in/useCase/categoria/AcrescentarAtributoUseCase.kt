package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.DomainId

fun interface AcrescentarAtributoUseCase {
    fun executar(comando: AcrescentarAtributoCommand): CategoriaDeInsumo
}

data class AcrescentarAtributoCommand(
    val categoriaId: DomainId,
    val definicao: DefinicaoDeAtributo,
)
