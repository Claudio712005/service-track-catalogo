package com.clau.service_track.catalogo.domain.model

data class ResultadoDeReserva(
    val reserva: Reserva,
    val movimento: MovimentoDeEstoque,
)
