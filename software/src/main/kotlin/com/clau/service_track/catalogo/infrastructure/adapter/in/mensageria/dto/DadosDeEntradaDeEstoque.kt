package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.dto

import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import java.math.BigDecimal

data class DadosDeEntradaDeEstoque(
    val insumoId: String,
    val quantidade: BigDecimal,
    val custoUnitario: BigDecimal? = null,
    val origemTipo: OrigemDeMovimento = OrigemDeMovimento.NOTA_ENTRADA,
    val origemId: String? = null,
    val registradoPor: String? = null,
)
