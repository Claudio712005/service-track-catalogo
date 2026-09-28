package com.clau.service_track.catalogo.domain.model

import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import java.math.BigDecimal
import java.time.LocalDateTime

class Reserva private constructor(
    val id: DomainId,
    val insumoId: DomainId,
    val ordemServicoId: DomainId,
    val quantidade: BigDecimal,
    val expiraEm: LocalDateTime?,
    val dataCriacao: LocalDateTime,
    status: StatusDeReserva,
    dataEncerramento: LocalDateTime?,
) {

    var status: StatusDeReserva = status
        private set

    var dataEncerramento: LocalDateTime? = dataEncerramento
        private set

    val ativa: Boolean
        get() = status == StatusDeReserva.ATIVA

    fun expirada(momento: LocalDateTime): Boolean =
        ativa && expiraEm != null && expiraEm.isBefore(momento)

    internal fun encerrar(novoStatus: StatusDeReserva, momento: LocalDateTime) {
        status = novoStatus
        dataEncerramento = momento
    }

    override fun equals(other: Any?): Boolean = other is Reserva && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Reserva(ordemServicoId=$ordemServicoId, status=$status)"

    companion object {

        fun criar(
            insumoId: DomainId,
            ordemServicoId: DomainId,
            quantidade: BigDecimal,
            expiraEm: LocalDateTime?,
        ): Reserva = Reserva(
            id = DomainId.gerar(),
            insumoId = insumoId,
            ordemServicoId = ordemServicoId,
            quantidade = quantidade,
            expiraEm = expiraEm,
            dataCriacao = LocalDateTime.now(),
            status = StatusDeReserva.ATIVA,
            dataEncerramento = null,
        )

        fun reconstituir(
            id: DomainId,
            insumoId: DomainId,
            ordemServicoId: DomainId,
            quantidade: BigDecimal,
            status: StatusDeReserva,
            expiraEm: LocalDateTime?,
            dataCriacao: LocalDateTime,
            dataEncerramento: LocalDateTime?,
        ): Reserva = Reserva(id, insumoId, ordemServicoId, quantidade, expiraEm, dataCriacao, status, dataEncerramento)
    }
}
