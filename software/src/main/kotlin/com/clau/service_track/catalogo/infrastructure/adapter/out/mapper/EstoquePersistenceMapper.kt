package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.application.port.out.repository.EventoParaPublicar
import com.clau.service_track.catalogo.domain.model.MovimentoDeEstoque
import com.clau.service_track.catalogo.domain.model.Reserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.infrastructure.entity.postgres.MovimentoEstoqueEntity
import com.clau.service_track.catalogo.infrastructure.entity.postgres.OutboxEntity
import com.clau.service_track.catalogo.infrastructure.entity.postgres.ReservaEstoqueEntity
import com.clau.service_track.catalogo.infrastructure.entity.postgres.SaldoEstoqueEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class EstoquePersistenceMapper {

    fun paraEntidade(saldo: SaldoDeInsumo): SaldoEstoqueEntity = SaldoEstoqueEntity(
        insumoId = UUID.fromString(saldo.insumoId.value),
        quantidadeDisponivel = saldo.quantidadeDisponivel,
        quantidadeReservada = saldo.quantidadeReservada,
        estoqueMinimo = saldo.estoqueMinimo,
        unidadeMedida = saldo.unidadeDeMedida,
        dataAtualizacao = paraOffset(saldo.dataAtualizacao),
    )

    fun atualizarEntidade(entidade: SaldoEstoqueEntity, saldo: SaldoDeInsumo): SaldoEstoqueEntity = entidade.apply {
        quantidadeDisponivel = saldo.quantidadeDisponivel
        quantidadeReservada = saldo.quantidadeReservada
        estoqueMinimo = saldo.estoqueMinimo
        dataAtualizacao = paraOffset(saldo.dataAtualizacao)
    }

    fun paraEntidade(reserva: Reserva): ReservaEstoqueEntity = ReservaEstoqueEntity(
        id = UUID.fromString(reserva.id.value),
        insumoId = UUID.fromString(reserva.insumoId.value),
        ordemServicoId = UUID.fromString(reserva.ordemServicoId.value),
        quantidade = reserva.quantidade,
        status = reserva.status,
        expiraEm = reserva.expiraEm?.let(::paraOffset),
        dataCriacao = paraOffset(reserva.dataCriacao),
        dataEncerramento = reserva.dataEncerramento?.let(::paraOffset),
    )

    fun atualizarEntidade(entidade: ReservaEstoqueEntity, reserva: Reserva): ReservaEstoqueEntity = entidade.apply {
        status = reserva.status
        dataEncerramento = reserva.dataEncerramento?.let(::paraOffset)
    }

    fun paraEntidade(movimento: MovimentoDeEstoque): MovimentoEstoqueEntity = MovimentoEstoqueEntity(
        id = UUID.fromString(movimento.id.value),
        insumoId = UUID.fromString(movimento.insumoId.value),
        reservaId = movimento.reservaId?.let { UUID.fromString(it.value) },
        tipo = movimento.tipo,
        quantidade = movimento.quantidade,
        unidadeMedida = movimento.unidadeDeMedida,
        custoUnitario = movimento.custoUnitario?.valor,
        saldoDisponivelApos = movimento.saldoDisponivelApos,
        origemTipo = movimento.origemTipo,
        origemId = movimento.origemId?.let { UUID.fromString(it.value) },
        chaveIdempotencia = movimento.chaveIdempotencia,
        registradoPor = movimento.registradoPor?.let { UUID.fromString(it.value) },
        dataCriacao = paraOffset(movimento.dataCriacao),
    )

    fun paraEntidade(evento: EventoParaPublicar): OutboxEntity = OutboxEntity(
        id = UUID.fromString(evento.idMensagem),
        agregadoTipo = evento.agregadoTipo,
        agregadoId = UUID.fromString(evento.agregadoId.value),
        chaveParticao = evento.chaveDeParticao,
        tipoEvento = evento.tipoEvento,
        versaoEvento = evento.versaoEvento,
        payload = evento.payload,
        traceId = evento.traceId,
        dataCriacao = OffsetDateTime.now(ZoneOffset.UTC),
    )

    fun paraDominio(saldo: SaldoEstoqueEntity, reservas: List<ReservaEstoqueEntity>): SaldoDeInsumo =
        SaldoDeInsumo.reconstituir(
            insumoId = DomainId.de(saldo.insumoId.toString()),
            unidadeDeMedida = saldo.unidadeMedida,
            quantidadeDisponivel = saldo.quantidadeDisponivel,
            quantidadeReservada = saldo.quantidadeReservada,
            estoqueMinimo = saldo.estoqueMinimo,
            versao = saldo.versao,
            dataAtualizacao = paraLocal(saldo.dataAtualizacao),
            reservas = reservas.map(::paraDominio),
        )

    fun paraDominio(reserva: ReservaEstoqueEntity): Reserva = Reserva.reconstituir(
        id = DomainId.de(reserva.id.toString()),
        insumoId = DomainId.de(reserva.insumoId.toString()),
        ordemServicoId = DomainId.de(reserva.ordemServicoId.toString()),
        quantidade = reserva.quantidade,
        status = reserva.status,
        expiraEm = reserva.expiraEm?.let(::paraLocal),
        dataCriacao = paraLocal(reserva.dataCriacao),
        dataEncerramento = reserva.dataEncerramento?.let(::paraLocal),
    )

    private fun paraOffset(momento: LocalDateTime): OffsetDateTime = momento.atOffset(ZoneOffset.UTC)

    private fun paraLocal(momento: OffsetDateTime): LocalDateTime =
        momento.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
}
