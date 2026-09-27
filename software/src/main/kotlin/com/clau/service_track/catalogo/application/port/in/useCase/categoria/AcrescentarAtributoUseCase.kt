package com.clau.service_track.catalogo.application.port.`in`.useCase.categoria

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo

fun interface AcrescentarAtributoUseCase {
    fun executar(comando: AcrescentarAtributoCommand): CategoriaDeInsumo
}
