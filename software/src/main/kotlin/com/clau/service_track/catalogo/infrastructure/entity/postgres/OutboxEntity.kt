package com.clau.service_track.catalogo.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "OUTBOX", schema = "CATALOGO")
class OutboxEntity(
    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "AGREGADO_TIPO", nullable = false, length = 40)
    var agregadoTipo: String,

    @Column(name = "AGREGADO_ID", nullable = false)
    var agregadoId: UUID,

    @Column(name = "CHAVE_PARTICAO", nullable = false, length = 60)
    var chaveParticao: String,

    @Column(name = "TIPO_EVENTO", nullable = false, length = 60)
    var tipoEvento: String,

    @Column(name = "VERSAO_EVENTO", nullable = false)
    var versaoEvento: Short = 1,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "PAYLOAD", nullable = false)
    var payload: String,

    @Column(name = "TRACE_ID", length = 64)
    var traceId: String? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_PUBLICACAO")
    var dataPublicacao: OffsetDateTime? = null,
)
