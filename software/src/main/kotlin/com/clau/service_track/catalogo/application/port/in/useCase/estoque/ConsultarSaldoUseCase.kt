package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

fun interface ConsultarSaldoUseCase {
    fun executar(consulta: ConsultarSaldoQuery): ResultadoDeSaldo
}
