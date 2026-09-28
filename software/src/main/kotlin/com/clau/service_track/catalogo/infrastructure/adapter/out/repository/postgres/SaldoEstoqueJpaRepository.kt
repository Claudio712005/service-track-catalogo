package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.infrastructure.entity.postgres.SaldoEstoqueEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SaldoEstoqueJpaRepository : JpaRepository<SaldoEstoqueEntity, UUID>
