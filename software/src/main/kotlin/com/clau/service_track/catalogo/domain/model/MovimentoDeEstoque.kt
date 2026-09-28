package com.clau.service_track.catalogo.domain.model

import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.TipoDeMovimento
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime

data class MovimentoDeEstoque(
    val id: DomainId,
    val insumoId: DomainId,
    val reservaId: DomainId?,
    val tipo: TipoDeMovimento,
    val quantidade: BigDecimal,
    val unidadeDeMedida: UnidadeDeMedida,
    val custoUnitario: ValorMonetario?,
    val saldoDisponivelApos: BigDecimal,
    val origemTipo: OrigemDeMovimento,
    val origemId: DomainId?,
    val chaveIdempotencia: String,
    val registradoPor: DomainId?,
    val dataCriacao: LocalDateTime,
)
