package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo

fun interface CriarCategoriaUseCase {
    fun executar(comando: CriarCategoriaCommand): CategoriaDeInsumo
}
