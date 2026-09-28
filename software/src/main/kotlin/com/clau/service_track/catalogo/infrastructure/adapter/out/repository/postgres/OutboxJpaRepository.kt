package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.infrastructure.entity.postgres.OutboxEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface OutboxJpaRepository : JpaRepository<OutboxEntity, UUID> {

    @Query(
        value = "SELECT * FROM CATALOGO.OUTBOX WHERE DATA_PUBLICACAO IS NULL " +
            "ORDER BY DATA_CRIACAO LIMIT :maximo FOR UPDATE SKIP LOCKED",
        nativeQuery = true,
    )
    fun reservarPendentes(maximo: Int): List<OutboxEntity>

    fun countByDataPublicacaoIsNull(): Long
}
