package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class DadosDeReservarEstoque(
    val insumoId: String,
    val ordemServicoId: String,
    val quantidade: BigDecimal,
    val expiraEm: LocalDateTime? = null,
)
