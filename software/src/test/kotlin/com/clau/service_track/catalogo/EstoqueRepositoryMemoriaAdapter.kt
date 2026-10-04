package com.clau.service_track.catalogo

import com.clau.service_track.catalogo.application.port.out.mensageria.RegistroDeMensagemPort
import com.clau.service_track.catalogo.application.port.out.repository.AlteracaoDeEstoque
import com.clau.service_track.catalogo.application.port.out.repository.EstoqueRepositoryPort
import com.clau.service_track.catalogo.application.port.out.repository.EventoParaPublicar
import com.clau.service_track.catalogo.domain.model.MovimentoDeEstoque
import com.clau.service_track.catalogo.domain.model.Reserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Repository
@Profile("teste")
class EstoqueRepositoryMemoriaAdapter : EstoqueRepositoryPort, RegistroDeMensagemPort {

    private val saldos = ConcurrentHashMap<String, SaldoDeInsumo>()
    private val processadas = ConcurrentHashMap<String, String>()

    val movimentos = CopyOnWriteArrayList<MovimentoDeEstoque>()
    val eventos = CopyOnWriteArrayList<EventoParaPublicar>()

    fun reiniciar() {
        saldos.clear()
        processadas.clear()
        movimentos.clear()
        eventos.clear()
    }

    fun semear(saldo: SaldoDeInsumo) {
        saldos[saldo.insumoId.value] = saldo
    }

    override fun buscarSaldo(insumoId: DomainId): SaldoDeInsumo? = saldos[insumoId.value]

    override fun listarReservasExpiradas(limite: LocalDateTime, maximo: Int): List<Reserva> = saldos.values
        .flatMap { it.reservasAtivas }
        .filter { it.expirada(limite) }
        .take(maximo)

    override fun aplicar(alteracao: AlteracaoDeEstoque) {
        saldos[alteracao.saldo.insumoId.value] = alteracao.saldo
        movimentos += alteracao.movimento
        processadas[identidadeNoInbox(alteracao.tipoDaMensagem, alteracao.chaveDeIdempotencia)] =
            alteracao.tipoDaMensagem
        alteracao.eventoParaPublicar?.let { eventos += it }
    }

    override fun registrarSemEfeito(
        chaveDeIdempotencia: String,
        tipoDaMensagem: String,
        evento: EventoParaPublicar?,
    ) {
        processadas[identidadeNoInbox(tipoDaMensagem, chaveDeIdempotencia)] = tipoDaMensagem
        evento?.let { eventos += it }
    }

    override fun jaProcessada(tipoDaMensagem: String, chave: String): Boolean =
        processadas.containsKey(identidadeNoInbox(tipoDaMensagem, chave))

    private fun identidadeNoInbox(tipoDaMensagem: String, chave: String) = "$tipoDaMensagem:$chave"
}
