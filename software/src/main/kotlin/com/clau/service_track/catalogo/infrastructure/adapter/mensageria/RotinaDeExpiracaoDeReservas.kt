package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ExpirarReservasCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ExpirarReservasUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "servicetrack.mensageria",
    name = ["habilitada", "expiracao-de-reservas.habilitada"],
    havingValue = "true",
)
class RotinaDeExpiracaoDeReservas(
    private val expirar: ExpirarReservasUseCase,
    private val propriedades: PropriedadesDeMensageria,
) {

    fun expirarVencidas(): Int = expirar.executar(ExpirarReservasCommand(propriedades.expiracaoDeReservas.lote))
}
