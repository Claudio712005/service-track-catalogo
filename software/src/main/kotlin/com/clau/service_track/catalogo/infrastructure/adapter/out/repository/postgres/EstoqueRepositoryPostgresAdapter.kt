package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.application.port.out.mensageria.RegistroDeMensagemPort
import com.clau.service_track.catalogo.application.port.out.repository.AlteracaoDeEstoque
import com.clau.service_track.catalogo.application.port.out.repository.EstoqueRepositoryPort
import com.clau.service_track.catalogo.application.port.out.repository.EventoParaPublicar
import com.clau.service_track.catalogo.domain.model.Reserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.EstoquePersistenceMapper
import com.clau.service_track.catalogo.infrastructure.entity.postgres.InboxEntity
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
@Profile("!teste")
class EstoqueRepositoryPostgresAdapter(
    private val saldos: SaldoEstoqueJpaRepository,
    private val reservas: ReservaEstoqueJpaRepository,
    private val movimentos: MovimentoEstoqueJpaRepository,
    private val inbox: InboxJpaRepository,
    private val outbox: OutboxJpaRepository,
    private val mapper: EstoquePersistenceMapper,
) : EstoqueRepositoryPort, RegistroDeMensagemPort {

    @Transactional(readOnly = true)
    override fun buscarSaldo(insumoId: DomainId): SaldoDeInsumo? {
        val identificador = UUID.fromString(insumoId.value)
        val saldo = saldos.findById(identificador).orElse(null) ?: return null
        val ativas = reservas.findAllByInsumoIdAndStatus(identificador, StatusDeReserva.ATIVA)
        return mapper.paraDominio(saldo, ativas)
    }

    @Transactional(readOnly = true)
    override fun listarReservasExpiradas(limite: LocalDateTime, maximo: Int): List<Reserva> = reservas
        .findAllByStatusAndExpiraEmLessThanOrderByExpiraEm(
            StatusDeReserva.ATIVA,
            limite.atOffset(ZoneOffset.UTC),
            Limit.of(maximo),
        )
        .map(mapper::paraDominio)

    @Transactional
    override fun aplicar(alteracao: AlteracaoDeEstoque) {
        gravarSaldo(alteracao.saldo)
        alteracao.reservaAfetada?.let(::gravarReserva)
        movimentos.save(mapper.paraEntidade(alteracao.movimento))
        registrar(alteracao.chaveDeIdempotencia, alteracao.tipoDaMensagem)
        alteracao.eventoParaPublicar?.let(::enfileirar)
    }

    @Transactional
    override fun registrarSemEfeito(
        chaveDeIdempotencia: String,
        tipoDaMensagem: String,
        evento: EventoParaPublicar?,
    ) {
        registrar(chaveDeIdempotencia, tipoDaMensagem)
        evento?.let(::enfileirar)
    }

    @Transactional(readOnly = true)
    override fun jaProcessada(chave: String): Boolean = inbox.existsById(chave)

    private fun gravarSaldo(saldo: SaldoDeInsumo) {
        val existente = saldos.findById(UUID.fromString(saldo.insumoId.value)).orElse(null)
        saldos.save(
            if (existente == null) mapper.paraEntidade(saldo) else mapper.atualizarEntidade(existente, saldo)
        )
    }

    private fun gravarReserva(reserva: Reserva) {
        val existente = reservas.findById(UUID.fromString(reserva.id.value)).orElse(null)
        reservas.save(
            if (existente == null) mapper.paraEntidade(reserva) else mapper.atualizarEntidade(existente, reserva)
        )
    }

    private fun registrar(chave: String, tipoDaMensagem: String) {
        inbox.save(
            InboxEntity(
                id = chave,
                tipoEvento = tipoDaMensagem,
                dataProcessamento = OffsetDateTime.now(ZoneOffset.UTC),
            )
        )
    }

    private fun enfileirar(evento: EventoParaPublicar) {
        outbox.save(mapper.paraEntidade(evento))
    }
}
