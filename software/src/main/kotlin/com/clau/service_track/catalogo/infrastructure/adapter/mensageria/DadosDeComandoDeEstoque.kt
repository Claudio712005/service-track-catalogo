    package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import java.math.BigDecimal
import java.time.LocalDateTime

data class DadosDeReservarEstoque(
    val insumoId: String,
    val ordemServicoId: String,
    val quantidade: BigDecimal,
    val expiraEm: LocalDateTime? = null,
)

data class DadosDeReservaEmAndamento(
    val insumoId: String,
    val ordemServicoId: String,
)

data class DadosDeEntradaDeEstoque(
    val insumoId: String,
    val quantidade: BigDecimal,
    val custoUnitario: BigDecimal? = null,
    val origemTipo: OrigemDeMovimento = OrigemDeMovimento.NOTA_ENTRADA,
    val origemId: String? = null,
    val registradoPor: String? = null,
)
