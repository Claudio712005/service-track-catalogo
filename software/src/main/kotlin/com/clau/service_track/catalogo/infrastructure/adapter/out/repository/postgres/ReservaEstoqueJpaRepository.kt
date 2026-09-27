package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import com.clau.service_track.catalogo.infrastructure.entity.postgres.ReservaEstoqueEntity
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ReservaEstoqueJpaRepository : JpaRepository<ReservaEstoqueEntity, UUID> {

    fun findAllByInsumoIdAndStatus(insumoId: UUID, status: StatusDeReserva): List<ReservaEstoqueEntity>

    fun findAllByStatusAndExpiraEmLessThanOrderByExpiraEm(
        status: StatusDeReserva,
        limite: OffsetDateTime,
        limit: Limit,
    ): List<ReservaEstoqueEntity>
}
