package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.infrastructure.entity.postgres.InboxEntity
import org.springframework.data.jpa.repository.JpaRepository

interface InboxJpaRepository : JpaRepository<InboxEntity, String>
