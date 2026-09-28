package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal

data class RegistrarEntradaCommand(
    val insumoId: DomainId,
    val quantidade: BigDecimal,
    val custoUnitario: ValorMonetario?,
    val origemTipo: OrigemDeMovimento,
    val origemId: DomainId?,
    val chaveIdempotencia: String,
    val registradoPor: DomainId?,
)
