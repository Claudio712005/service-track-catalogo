package com.clau.service_track.catalogo.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "INBOX", schema = "CATALOGO")
class InboxEntity(
    @Id
    @Column(name = "ID", nullable = false, length = 120)
    var id: String,

    @Column(name = "TIPO_EVENTO", nullable = false, length = 60)
    var tipoEvento: String,

    @Column(name = "DATA_PROCESSAMENTO", nullable = false)
    var dataProcessamento: OffsetDateTime,
)
