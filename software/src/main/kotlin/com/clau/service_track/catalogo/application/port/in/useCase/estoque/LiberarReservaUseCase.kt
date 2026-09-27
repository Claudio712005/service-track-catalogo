package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

fun interface LiberarReservaUseCase {
    fun executar(comando: LiberarReservaCommand): ResultadoDoPasso
}
