package com.clau.service_track.catalogo.application.port.out.repository

import com.clau.service_track.catalogo.domain.model.MovimentoDeEstoque
import com.clau.service_track.catalogo.domain.model.Reserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo

data class AlteracaoDeEstoque(
    val saldo: SaldoDeInsumo,
    val movimento: MovimentoDeEstoque,
    val reservaAfetada: Reserva?,
    val chaveDeIdempotencia: String,
    val tipoDaMensagem: String,
    val eventoParaPublicar: EventoParaPublicar?,
)
