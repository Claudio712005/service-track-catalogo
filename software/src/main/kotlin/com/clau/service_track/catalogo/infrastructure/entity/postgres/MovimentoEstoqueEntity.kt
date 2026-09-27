package com.clau.service_track.catalogo.infrastructure.entity.postgres

import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.TipoDeMovimento
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ESTOQUE_MOVIMENTOS", schema = "CATALOGO")
class MovimentoEstoqueEntity(
    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "INSUMO_ID", nullable = false)
    var insumoId: UUID,

    @Column(name = "LOTE_ID")
    var loteId: UUID? = null,

    @Column(name = "RESERVA_ID")
    var reservaId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    var tipo: TipoDeMovimento,

    @Column(name = "QUANTIDADE", nullable = false, precision = 14, scale = 4)
    var quantidade: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "UNIDADE_MEDIDA", nullable = false, length = 20)
    var unidadeMedida: UnidadeDeMedida,

    @Column(name = "CUSTO_UNITARIO", precision = 12, scale = 2)
    var custoUnitario: BigDecimal? = null,

    @Column(name = "SALDO_DISPONIVEL_APOS", nullable = false, precision = 14, scale = 4)
    var saldoDisponivelApos: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "ORIGEM_TIPO", nullable = false, length = 30)
    var origemTipo: OrigemDeMovimento,

    @Column(name = "ORIGEM_ID")
    var origemId: UUID? = null,

    @Column(name = "CHAVE_IDEMPOTENCIA", nullable = false, length = 120)
    var chaveIdempotencia: String,

    @Column(name = "REGISTRADO_POR")
    var registradoPor: UUID? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,
)
