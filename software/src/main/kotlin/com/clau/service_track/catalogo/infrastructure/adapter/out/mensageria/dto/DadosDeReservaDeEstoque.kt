package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class DadosDeReservaDeEstoque(
    val insumoId: String,
    val sku: String,
    val ordemServicoId: String,
    val reservaId: String,
    val quantidade: BigDecimal,
    val unidadeDeMedida: String,
    val expiraEm: OffsetDateTime?,
    val saldoDisponivel: BigDecimal,
    val saldoReservado: BigDecimal,
)
