package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.infrastructure.entity.postgres.ServicoEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServicoJpaRepository : JpaRepository<ServicoEntity, UUID> {

    fun findAllByAtivoTrue(): List<ServicoEntity>

    fun existsByNomeServicoIgnoreCaseAndAtivoTrue(nomeServico: String): Boolean
}
