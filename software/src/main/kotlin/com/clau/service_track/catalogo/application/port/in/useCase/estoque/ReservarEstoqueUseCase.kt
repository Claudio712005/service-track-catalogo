package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

fun interface ReservarEstoqueUseCase {
    fun executar(comando: ReservarEstoqueCommand): ResultadoDoPasso
}
