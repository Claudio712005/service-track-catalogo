package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo

data class ResultadoDeSaldo(
    val insumo: Insumo,
    val saldo: SaldoDeInsumo,
)
