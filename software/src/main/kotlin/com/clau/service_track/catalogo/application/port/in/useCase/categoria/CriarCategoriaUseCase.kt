package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida

fun interface CriarCategoriaUseCase {
    fun executar(comando: CriarCategoriaCommand): CategoriaDeInsumo
}

data class CriarCategoriaCommand(
    val codigo: String,
    val nome: String,
    val unidadePadrao: UnidadeDeMedida,
    val atributos: List<DefinicaoDeAtributo>,
)
