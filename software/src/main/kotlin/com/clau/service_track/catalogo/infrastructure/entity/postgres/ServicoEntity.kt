package com.clau.service_track.catalogo.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "SERVICOS", schema = "CATALOGO")
class ServicoEntity(
    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "NOME_SERVICO", nullable = false, length = 150)
    var nomeServico: String,

    @Column(name = "DESCRICAO_SERVICO", nullable = false, columnDefinition = "TEXT")
    var descricaoServico: String,

    @Column(name = "VALOR_REFERENCIA", precision = 12, scale = 2)
    var valorReferencia: BigDecimal? = null,

    @Column(name = "ATIVO", nullable = false)
    var ativo: Boolean = true,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,
)
