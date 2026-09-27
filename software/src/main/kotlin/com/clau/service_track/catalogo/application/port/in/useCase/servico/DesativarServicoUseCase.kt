package com.clau.service_track.catalogo.application.port.`in`.useCase.servico

fun interface DesativarServicoUseCase {
    fun executar(comando: DesativarServicoCommand)
}
