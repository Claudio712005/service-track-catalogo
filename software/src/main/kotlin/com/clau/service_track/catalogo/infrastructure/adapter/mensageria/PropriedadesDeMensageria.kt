package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "servicetrack.mensageria")
data class PropriedadesDeMensageria(

    val habilitada: Boolean = false,

    val grupo: String = "catalogo-estoque",

    val topicoDeComandos: String = "servicetrack.estoque.comandos.v1",

    val topicoDeEventos: String = "servicetrack.estoque.eventos.v1",

    val sufixoDaDlt: String = ".dlt",

    val concorrencia: Int = 1,

    val tentativas: Int = 4,

    val esperaInicial: Duration = Duration.ofMillis(500),

    val esperaMaxima: Duration = Duration.ofSeconds(5),

    val multiplicadorDaEspera: Double = 2.0,

    val intervaloDePublicacao: Duration = Duration.ofSeconds(2),

    val loteDePublicacao: Int = 50,

    val expiracaoDeReservas: ExpiracaoDeReservas = ExpiracaoDeReservas(),
) {

    val topicoDaDlt: String
        get() = topicoDeComandos + sufixoDaDlt

    data class ExpiracaoDeReservas(

        val habilitada: Boolean = true,

        val intervalo: Duration = Duration.ofSeconds(60),

        val lote: Int = 100,
    )
}
