package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

fun interface RegistrarEntradaUseCase {
    fun executar(comando: RegistrarEntradaCommand): ResultadoDeSaldo
}
