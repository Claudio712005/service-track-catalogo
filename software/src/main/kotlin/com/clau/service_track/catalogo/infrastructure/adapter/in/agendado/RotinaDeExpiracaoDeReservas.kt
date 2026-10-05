package com.clau.service_track.catalogo.infrastructure.adapter.`in`.agendado

import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ExpirarReservasCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ExpirarReservasUseCase
import com.clau.service_track.catalogo.infrastructure.adapter.config.mensageria.MensageriaProperties
import com.clau.service_track.catalogo.infrastructure.adapter.out.observabilidade.MetricasDeEstoque
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
    private val propriedades: MensageriaProperties,
    private val metricas: MetricasDeEstoque,
) {

    fun expirarVencidas(): Int {
        val expiradas = expirar.executar(ExpirarReservasCommand(propriedades.expiracaoDeReservas.lote))
        metricas.reservasExpiradas(expiradas)
        return expiradas
    }
}
