package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

fun interface ExpirarReservasUseCase {
    fun executar(comando: ExpirarReservasCommand): Int
}
