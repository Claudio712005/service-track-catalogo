package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import com.clau.service_track.catalogo.infrastructure.entity.postgres.InboxEntity
import com.clau.service_track.catalogo.infrastructure.entity.postgres.MovimentoEstoqueEntity
import com.clau.service_track.catalogo.infrastructure.entity.postgres.OutboxEntity
import com.clau.service_track.catalogo.infrastructure.entity.postgres.ReservaEstoqueEntity
import com.clau.service_track.catalogo.infrastructure.entity.postgres.SaldoEstoqueEntity
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime
import java.util.UUID

interface SaldoEstoqueJpaRepository : JpaRepository<SaldoEstoqueEntity, UUID>

interface ReservaEstoqueJpaRepository : JpaRepository<ReservaEstoqueEntity, UUID> {

    fun findAllByInsumoIdAndStatus(insumoId: UUID, status: StatusDeReserva): List<ReservaEstoqueEntity>

    fun findAllByStatusAndExpiraEmLessThanOrderByExpiraEm(
        status: StatusDeReserva,
        limite: OffsetDateTime,
        limit: Limit,
    ): List<ReservaEstoqueEntity>
}

interface MovimentoEstoqueJpaRepository : JpaRepository<MovimentoEstoqueEntity, UUID>

interface InboxJpaRepository : JpaRepository<InboxEntity, String>

interface OutboxJpaRepository : JpaRepository<OutboxEntity, UUID> {

    @Query(
        value = "SELECT * FROM CATALOGO.OUTBOX WHERE DATA_PUBLICACAO IS NULL " +
            "ORDER BY DATA_CRIACAO LIMIT :maximo FOR UPDATE SKIP LOCKED",
        nativeQuery = true,
    )
    fun reservarPendentes(maximo: Int): List<OutboxEntity>
}
