package com.clau.service_track.catalogo.infrastructure.adapter.config.mensageria

import java.time.Duration

data class ExpiracaoDeReservas(

    val habilitada: Boolean = true,

    val intervalo: Duration = Duration.ofSeconds(60),

    val lote: Int = 100,
)
