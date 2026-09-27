package com.clau.service_track.catalogo.infrastructure.entity.postgres

import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ESTOQUE_SALDOS", schema = "CATALOGO")
class SaldoEstoqueEntity(
    @Id
    @Column(name = "INSUMO_ID", nullable = false)
    var insumoId: UUID,

    @Column(name = "QUANTIDADE_DISPONIVEL", nullable = false, precision = 14, scale = 4)
    var quantidadeDisponivel: BigDecimal = BigDecimal.ZERO,

    @Column(name = "QUANTIDADE_RESERVADA", nullable = false, precision = 14, scale = 4)
    var quantidadeReservada: BigDecimal = BigDecimal.ZERO,

    @Column(name = "ESTOQUE_MINIMO", nullable = false, precision = 14, scale = 4)
    var estoqueMinimo: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "UNIDADE_MEDIDA", nullable = false, length = 20)
    var unidadeMedida: UnidadeDeMedida,

    @Version
    @Column(name = "VERSAO", nullable = false)
    var versao: Int = 0,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,
)
