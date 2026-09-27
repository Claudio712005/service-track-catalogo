package com.clau.service_track.catalogo.application.handler.estoque

import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueCommand
import com.clau.service_track.catalogo.application.port.out.repository.EventoParaPublicar
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.model.ResultadoDeReserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo

interface FabricaDeEventoDeEstoque {

    fun estoqueReservado(
        insumo: Insumo,
        resultado: ResultadoDeReserva,
        saldo: SaldoDeInsumo,
        traceId: String?,
    ): EventoParaPublicar

    fun estoqueConsumido(
        insumo: Insumo,
        resultado: ResultadoDeReserva,
        saldo: SaldoDeInsumo,
        traceId: String?,
    ): EventoParaPublicar

    fun reservaLiberada(
        insumo: Insumo,
        resultado: ResultadoDeReserva,
        saldo: SaldoDeInsumo,
        traceId: String?,
    ): EventoParaPublicar

    fun reservaExpirada(
        insumo: Insumo,
        resultado: ResultadoDeReserva,
        saldo: SaldoDeInsumo,
        traceId: String?,
    ): EventoParaPublicar

    fun reservaRecusada(insumo: Insumo, comando: ReservarEstoqueCommand, motivo: String): EventoParaPublicar
}
