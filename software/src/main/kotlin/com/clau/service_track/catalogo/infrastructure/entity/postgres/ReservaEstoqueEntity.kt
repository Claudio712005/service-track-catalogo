package com.clau.service_track.catalogo.infrastructure.entity.postgres

import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ESTOQUE_RESERVAS", schema = "CATALOGO")
class ReservaEstoqueEntity(
    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "INSUMO_ID", nullable = false)
    var insumoId: UUID,

    @Column(name = "ORDEM_SERVICO_ID", nullable = false)
    var ordemServicoId: UUID,

    @Column(name = "QUANTIDADE", nullable = false, precision = 14, scale = 4)
    var quantidade: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    var status: StatusDeReserva,

    @Column(name = "EXPIRA_EM")
    var expiraEm: OffsetDateTime? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ENCERRAMENTO")
    var dataEncerramento: OffsetDateTime? = null,
)
