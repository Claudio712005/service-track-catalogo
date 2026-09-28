package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

fun interface ConsumirReservaUseCase {
    fun executar(comando: ConsumirReservaCommand): ResultadoDoPasso
}
