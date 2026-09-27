package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida

data class CriarCategoriaCommand(
    val codigo: String,
    val nome: String,
    val unidadePadrao: UnidadeDeMedida,
    val atributos: List<DefinicaoDeAtributo>,
)
