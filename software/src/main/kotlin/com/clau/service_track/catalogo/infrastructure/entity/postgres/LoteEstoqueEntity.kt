package com.clau.service_track.catalogo.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ESTOQUE_LOTES", schema = "CATALOGO")
class LoteEstoqueEntity(
    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "INSUMO_ID", nullable = false)
    var insumoId: UUID,

    @Column(name = "CODIGO_LOTE", nullable = false, length = 60)
    var codigoLote: String,

    @Column(name = "VALIDADE")
    var validade: LocalDate? = null,

    @Column(name = "CUSTO_UNITARIO", nullable = false, precision = 12, scale = 2)
    var custoUnitario: BigDecimal,

    @Column(name = "QUANTIDADE_ATUAL", nullable = false, precision = 14, scale = 4)
    var quantidadeAtual: BigDecimal = BigDecimal.ZERO,

    @Column(name = "DATA_RECEBIMENTO", nullable = false)
    var dataRecebimento: OffsetDateTime,
)
