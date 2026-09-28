package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.vo.DomainId
import java.math.BigDecimal
import java.time.LocalDateTime

data class ReservarEstoqueCommand(
    val insumoId: DomainId,
    val ordemServicoId: DomainId,
    val quantidade: BigDecimal,
    val expiraEm: LocalDateTime?,
    val chaveIdempotencia: String,
    val traceId: String?,
)
