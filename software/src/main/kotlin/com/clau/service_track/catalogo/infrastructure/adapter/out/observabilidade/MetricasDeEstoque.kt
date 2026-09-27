package com.clau.service_track.catalogo.infrastructure.adapter.out.observabilidade

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry

class MetricasDeEstoque(
    private val registro: MeterRegistry,
    contarPendentesNoOutbox: () -> Long,
) {

    init {
        Gauge.builder(OUTBOX_PENDENTES) { contarPendentesNoOutbox() }
            .description("Eventos gravados no outbox e ainda nao publicados no broker")
            .strongReference(true)
            .register(registro)

        TIPOS.forEach { tipo -> RESULTADOS.forEach { resultado -> contador(tipo, resultado) } }
        DESFECHOS_DE_TIPO_DESCONHECIDO.forEach { contador(TIPO_DESCONHECIDO, it) }
        contadorDeEventos()
        contadorDeExpiradas()
    }

    fun comandoProcessado(tipo: String, resultado: String) {
        contador(tipo, resultado).increment()
    }

    private fun contador(tipo: String, resultado: String): Counter = Counter.builder(COMANDOS)
        .description("Comandos de estoque consumidos do Kafka, por tipo e desfecho")
        .tag("tipo", tipo)
        .tag("resultado", resultado)
        .register(registro)

    fun eventosPublicados(quantidade: Int) {
        contadorDeEventos().increment(quantidade.toDouble())
    }

    fun reservasExpiradas(quantidade: Int) {
        if (quantidade == 0) return
        contadorDeExpiradas().increment(quantidade.toDouble())
    }

    private fun contadorDeEventos(): Counter = Counter.builder(EVENTOS)
        .description("Eventos de estoque publicados a partir do outbox")
        .register(registro)

    private fun contadorDeExpiradas(): Counter = Counter.builder(EXPIRADAS)
        .description("Reservas devolvidas ao estoque por vencimento do prazo")
        .register(registro)

    private companion object {
        const val TIPO_DESCONHECIDO = "desconhecido"

        val TIPOS = listOf(
            "ReservarEstoque",
            "ConsumirReserva",
            "LiberarReserva",
            "RegistrarEntradaDeEstoque",
        )

        val RESULTADOS = listOf("aplicado", "ja_processado", "recusado", "invalido", "erro")

        val DESFECHOS_DE_TIPO_DESCONHECIDO = listOf("invalido", "erro")

        const val OUTBOX_PENDENTES = "catalogo.outbox.pendentes"
        const val COMANDOS = "catalogo.estoque.comandos"
        const val EVENTOS = "catalogo.estoque.eventos.publicados"
        const val EXPIRADAS = "catalogo.estoque.reservas.expiradas"
    }
}
