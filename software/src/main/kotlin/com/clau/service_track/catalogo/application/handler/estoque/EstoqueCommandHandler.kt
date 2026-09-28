package com.clau.service_track.catalogo.application.handler.estoque

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsumirReservaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsumirReservaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ExpirarReservasCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ExpirarReservasUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.LiberarReservaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.LiberarReservaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.RegistrarEntradaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.RegistrarEntradaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ResultadoDeSaldo
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ResultadoDoPasso
import com.clau.service_track.catalogo.application.port.out.mensageria.FabricaDeEventoDeEstoquePort
import com.clau.service_track.catalogo.application.port.out.mensageria.RegistroDeMensagemPort
import com.clau.service_track.catalogo.application.port.out.repository.AlteracaoDeEstoque
import com.clau.service_track.catalogo.application.port.out.repository.EstoqueRepositoryPort
import com.clau.service_track.catalogo.application.port.out.repository.EventoParaPublicar
import com.clau.service_track.catalogo.application.port.out.repository.InsumoRepositoryPort
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.model.ResultadoDeReserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.model.SaldoInsuficienteException
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.shared.annotation.UseCase
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@UseCase
class EstoqueCommandHandler(
    private val estoque: EstoqueRepositoryPort,
    private val insumos: InsumoRepositoryPort,
    private val mensagens: RegistroDeMensagemPort,
    private val eventos: FabricaDeEventoDeEstoquePort,
) : RegistrarEntradaUseCase,
    ReservarEstoqueUseCase,
    ConsumirReservaUseCase,
    LiberarReservaUseCase,
    ExpirarReservasUseCase {

    private val log = LoggerFactory.getLogger(EstoqueCommandHandler::class.java)

    override fun executar(comando: RegistrarEntradaCommand): ResultadoDeSaldo {
        val insumo = exigirInsumo(comando.insumoId)
        val saldo = saldoDe(insumo)

        if (mensagens.jaProcessada(comando.chaveIdempotencia)) {
            log.info("entrada de estoque ja registrada, nada a fazer chaveIdempotencia={}", comando.chaveIdempotencia)
            return ResultadoDeSaldo(insumo, saldo)
        }

        val movimento = saldo.registrarEntrada(
            quantidade = comando.quantidade,
            custoUnitario = comando.custoUnitario,
            origemTipo = comando.origemTipo,
            origemId = comando.origemId,
            chaveIdempotencia = comando.chaveIdempotencia,
            registradoPor = comando.registradoPor,
        )

        estoque.aplicar(
            AlteracaoDeEstoque(
                saldo = saldo,
                movimento = movimento,
                reservaAfetada = null,
                chaveDeIdempotencia = comando.chaveIdempotencia,
                tipoDaMensagem = TIPO_ENTRADA,
                eventoParaPublicar = null,
            )
        )

        log.info(
            "entrada de estoque registrada insumoId={} sku={} quantidade={} disponivel={}",
            insumo.id.value, insumo.sku, comando.quantidade, saldo.quantidadeDisponivel,
        )
        alertarSeAbaixoDoMinimo(insumo, saldo)
        return ResultadoDeSaldo(insumo, saldo)
    }

    override fun executar(comando: ReservarEstoqueCommand): ResultadoDoPasso {
        if (mensagens.jaProcessada(comando.chaveIdempotencia)) {
            return jaProcessado(comando.chaveIdempotencia)
        }

        val insumo = exigirInsumo(comando.insumoId)
        val saldo = saldoDe(insumo)

        val resultado = try {
            saldo.reservar(comando.ordemServicoId, comando.quantidade, comando.expiraEm, comando.chaveIdempotencia)
        } catch (e: SaldoInsuficienteException) {
            log.warn(
                "reserva recusada por saldo insuficiente insumoId={} sku={} ordemServicoId={} solicitado={} disponivel={}",
                insumo.id.value, insumo.sku, comando.ordemServicoId.value, e.solicitado, e.disponivel,
            )
            publicarRecusa(insumo, comando, e.message ?: "saldo insuficiente")
            return ResultadoDoPasso.recusado(e.message ?: "saldo insuficiente")
        }

        aplicar(insumo, saldo, resultado, TIPO_RESERVA, eventos.estoqueReservado(insumo, resultado, saldo, comando.traceId))

        log.info(
            "estoque reservado insumoId={} sku={} ordemServicoId={} quantidade={} disponivel={} reservada={}",
            insumo.id.value, insumo.sku, comando.ordemServicoId.value, resultado.reserva.quantidade,
            saldo.quantidadeDisponivel, saldo.quantidadeReservada,
        )
        alertarSeAbaixoDoMinimo(insumo, saldo)
        return ResultadoDoPasso.aplicado(saldo)
    }

    override fun executar(comando: ConsumirReservaCommand): ResultadoDoPasso {
        if (mensagens.jaProcessada(comando.chaveIdempotencia)) {
            return jaProcessado(comando.chaveIdempotencia)
        }

        val insumo = exigirInsumo(comando.insumoId)
        val saldo = saldoDe(insumo)
        val resultado = saldo.consumirReserva(comando.ordemServicoId, comando.chaveIdempotencia)

        aplicar(insumo, saldo, resultado, TIPO_CONSUMO, eventos.estoqueConsumido(insumo, resultado, saldo, comando.traceId))

        log.info(
            "reserva consumida insumoId={} sku={} ordemServicoId={} quantidade={} disponivel={}",
            insumo.id.value, insumo.sku, comando.ordemServicoId.value, resultado.reserva.quantidade,
            saldo.quantidadeDisponivel,
        )
        return ResultadoDoPasso.aplicado(saldo)
    }

    override fun executar(comando: LiberarReservaCommand): ResultadoDoPasso {
        if (mensagens.jaProcessada(comando.chaveIdempotencia)) {
            return jaProcessado(comando.chaveIdempotencia)
        }

        val insumo = exigirInsumo(comando.insumoId)
        val saldo = saldoDe(insumo)
        val resultado = saldo.liberarReserva(comando.ordemServicoId, comando.chaveIdempotencia)

        aplicar(insumo, saldo, resultado, TIPO_LIBERACAO, eventos.reservaLiberada(insumo, resultado, saldo, comando.traceId))

        log.info(
            "reserva liberada insumoId={} sku={} ordemServicoId={} quantidade={} disponivel={}",
            insumo.id.value, insumo.sku, comando.ordemServicoId.value, resultado.reserva.quantidade,
            saldo.quantidadeDisponivel,
        )
        return ResultadoDoPasso.aplicado(saldo)
    }

    override fun executar(comando: ExpirarReservasCommand): Int {
        val vencidas = estoque.listarReservasExpiradas(LocalDateTime.now(), comando.maximo)
        if (vencidas.isEmpty()) return 0

        return vencidas.count { vencida ->
            val insumo = insumos.buscarPorId(vencida.insumoId)
            if (insumo == null) {
                log.error(
                    "reserva aponta para insumo inexistente reservaId={} insumoId={}",
                    vencida.id.value, vencida.insumoId.value,
                )
                false
            } else {
                expirar(insumo, vencida.ordemServicoId)
            }
        }
    }

    private fun expirar(insumo: Insumo, ordemServicoId: DomainId): Boolean {
        val saldo = saldoDe(insumo)
        val noAgregado = saldo.reservaAtivaDe(ordemServicoId) ?: return false
        val chave = "expiracao:${noAgregado.id.value}"
        if (mensagens.jaProcessada(chave)) return false

        val resultado = saldo.expirarReserva(noAgregado, chave)
        aplicar(insumo, saldo, resultado, TIPO_EXPIRACAO, eventos.reservaExpirada(insumo, resultado, saldo, null))

        log.warn(
            "reserva expirada e devolvida ao estoque insumoId={} sku={} ordemServicoId={} quantidade={} disponivel={}",
            insumo.id.value, insumo.sku, ordemServicoId.value, resultado.reserva.quantidade,
            saldo.quantidadeDisponivel,
        )
        return true
    }

    private fun aplicar(
        insumo: Insumo,
        saldo: SaldoDeInsumo,
        resultado: ResultadoDeReserva,
        tipoDaMensagem: String,
        evento: EventoParaPublicar,
    ) {
        estoque.aplicar(
            AlteracaoDeEstoque(
                saldo = saldo,
                movimento = resultado.movimento,
                reservaAfetada = resultado.reserva,
                chaveDeIdempotencia = resultado.movimento.chaveIdempotencia,
                tipoDaMensagem = tipoDaMensagem,
                eventoParaPublicar = evento,
            )
        )
    }

    private fun publicarRecusa(insumo: Insumo, comando: ReservarEstoqueCommand, motivo: String) {
        estoque.registrarSemEfeito(
            chaveDeIdempotencia = comando.chaveIdempotencia,
            tipoDaMensagem = TIPO_RESERVA,
            evento = eventos.reservaRecusada(insumo, comando, motivo),
        )
    }

    private fun jaProcessado(chave: String): ResultadoDoPasso {
        log.info("mensagem ja processada, nada a fazer chaveIdempotencia={}", chave)
        return ResultadoDoPasso.jaProcessado()
    }

    private fun alertarSeAbaixoDoMinimo(insumo: Insumo, saldo: SaldoDeInsumo) {
        if (saldo.abaixoDoMinimo) {
            log.warn(
                "estoque abaixo do minimo insumoId={} sku={} disponivel={} minimo={}",
                insumo.id.value, insumo.sku, saldo.quantidadeDisponivel, saldo.estoqueMinimo,
            )
        }
    }

    private fun saldoDe(insumo: Insumo): SaldoDeInsumo =
        estoque.buscarSaldo(insumo.id) ?: SaldoDeInsumo.zerado(insumo.id, insumo.unidadeDeMedida)

    private fun exigirInsumo(id: DomainId): Insumo =
        insumos.buscarPorId(id) ?: throw RecursoNaoEncontradoException("Insumo", id.value)

    private companion object {
        const val TIPO_ENTRADA = "RegistrarEntradaDeEstoque"
        const val TIPO_RESERVA = "ReservarEstoque"
        const val TIPO_CONSUMO = "ConsumirReserva"
        const val TIPO_LIBERACAO = "LiberarReserva"
        const val TIPO_EXPIRACAO = "ExpirarReserva"
    }
}
