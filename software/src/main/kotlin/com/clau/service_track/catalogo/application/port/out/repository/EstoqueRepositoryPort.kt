package com.clau.service_track.catalogo.application.port.out.repository

import com.clau.service_track.catalogo.domain.model.MovimentoDeEstoque
import com.clau.service_track.catalogo.domain.model.Reserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import java.time.LocalDateTime

interface EstoqueRepositoryPort {

    fun buscarSaldo(insumoId: DomainId): SaldoDeInsumo?

    fun listarReservasExpiradas(limite: LocalDateTime, maximo: Int): List<Reserva>

    fun aplicar(alteracao: AlteracaoDeEstoque)

    fun registrarSemEfeito(chaveDeIdempotencia: String, tipoDaMensagem: String, evento: EventoParaPublicar?)
}

data class AlteracaoDeEstoque(
    val saldo: SaldoDeInsumo,
    val movimento: MovimentoDeEstoque,
    val reservaAfetada: Reserva?,
    val chaveDeIdempotencia: String,
    val tipoDaMensagem: String,
    val eventoParaPublicar: EventoParaPublicar?,
)

data class EventoParaPublicar(
    val idMensagem: String,
    val agregadoTipo: String,
    val agregadoId: DomainId,
    val chaveDeParticao: String,
    val tipoEvento: String,
    val versaoEvento: Short,
    val payload: String,
    val traceId: String?,
)
