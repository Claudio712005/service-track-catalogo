package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.infrastructure.entity.postgres.MovimentoEstoqueEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MovimentoEstoqueJpaRepository : JpaRepository<MovimentoEstoqueEntity, UUID>
