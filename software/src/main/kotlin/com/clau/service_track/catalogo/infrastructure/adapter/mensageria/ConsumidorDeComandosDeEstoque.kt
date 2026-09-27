package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsumirReservaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsumirReservaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.LiberarReservaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.LiberarReservaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.RegistrarEntradaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.RegistrarEntradaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueUseCase
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import com.clau.service_track.catalogo.infrastructure.adapter.web.filter.CorrelacaoFilter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnProperty(prefix = "servicetrack.mensageria", name = ["habilitada"], havingValue = "true")
class ConsumidorDeComandosDeEstoque(
    private val leitor: LeitorDeEnvelope,
    private val reservar: ReservarEstoqueUseCase,
    private val consumir: ConsumirReservaUseCase,
    private val liberar: LiberarReservaUseCase,
    private val registrarEntrada: RegistrarEntradaUseCase,
    private val metricas: MetricasDeEstoque,
) {

    private val log = LoggerFactory.getLogger(ConsumidorDeComandosDeEstoque::class.java)

    @KafkaListener(
        topics = ["\${servicetrack.mensageria.topico-de-comandos}"],
        containerFactory = "fabricaDeContainerDeComandos",
    )
    fun consumir(registro: ConsumerRecord<String, String>) {
        val envelope = leitor.ler(registro)
        anotarContexto(envelope)

        try {
            log.debug(
                "comando recebido tipo={} versao={} particao={} offset={}",
                envelope.tipo, envelope.versao, registro.partition(), registro.offset(),
            )
            metricas.comandoProcessado(envelope.tipo, despachar(envelope))
        } catch (e: MensagemInvalidaException) {
            metricas.comandoProcessado(tipoConhecido(envelope.tipo), INVALIDO)
            throw e
        } catch (e: Exception) {
            metricas.comandoProcessado(tipoConhecido(envelope.tipo), ERRO)
            throw e
        } finally {
            MDC.remove(CorrelacaoFilter.CHAVE_CORRELACAO)
            MDC.remove(CorrelacaoFilter.CHAVE_TRANSACAO)
        }
    }

    private fun despachar(envelope: EnvelopeDeMensagem): String = when (envelope.tipo) {
        RESERVAR -> reservar.executar(paraReserva(envelope)).situacao.name.lowercase()
        CONSUMIR -> consumir.executar(paraConsumo(envelope)).situacao.name.lowercase()
        LIBERAR -> liberar.executar(paraLiberacao(envelope)).situacao.name.lowercase()
        ENTRADA -> {
            registrarEntrada.executar(paraEntrada(envelope))
            APLICADO
        }

        else -> throw MensagemInvalidaException(
            "Comando '${envelope.tipo}' não é reconhecido por este serviço"
        )
    }

    private fun tipoConhecido(tipo: String): String = if (tipo in TIPOS) tipo else DESCONHECIDO

    private fun paraReserva(envelope: EnvelopeDeMensagem): ReservarEstoqueCommand {
        val dados = leitor.dados(envelope, DadosDeReservarEstoque::class.java)
        return ReservarEstoqueCommand(
            insumoId = DomainId.de(dados.insumoId),
            ordemServicoId = DomainId.de(dados.ordemServicoId),
            quantidade = dados.quantidade,
            expiraEm = dados.expiraEm,
            chaveIdempotencia = envelope.idMensagem,
            traceId = envelope.traceId,
        )
    }

    private fun paraConsumo(envelope: EnvelopeDeMensagem): ConsumirReservaCommand {
        val dados = leitor.dados(envelope, DadosDeReservaEmAndamento::class.java)
        return ConsumirReservaCommand(
            insumoId = DomainId.de(dados.insumoId),
            ordemServicoId = DomainId.de(dados.ordemServicoId),
            chaveIdempotencia = envelope.idMensagem,
            traceId = envelope.traceId,
        )
    }

    private fun paraLiberacao(envelope: EnvelopeDeMensagem): LiberarReservaCommand {
        val dados = leitor.dados(envelope, DadosDeReservaEmAndamento::class.java)
        return LiberarReservaCommand(
            insumoId = DomainId.de(dados.insumoId),
            ordemServicoId = DomainId.de(dados.ordemServicoId),
            chaveIdempotencia = envelope.idMensagem,
            traceId = envelope.traceId,
        )
    }

    private fun paraEntrada(envelope: EnvelopeDeMensagem): RegistrarEntradaCommand {
        val dados = leitor.dados(envelope, DadosDeEntradaDeEstoque::class.java)
        return RegistrarEntradaCommand(
            insumoId = DomainId.de(dados.insumoId),
            quantidade = dados.quantidade,
            custoUnitario = dados.custoUnitario?.let(ValorMonetario::de),
            origemTipo = dados.origemTipo,
            origemId = dados.origemId?.let(DomainId::de),
            chaveIdempotencia = envelope.idMensagem,
            registradoPor = dados.registradoPor?.let(DomainId::de),
        )
    }

    private fun anotarContexto(envelope: EnvelopeDeMensagem) {
        MDC.put(
            CorrelacaoFilter.CHAVE_CORRELACAO,
            envelope.correlationId ?: UUID.randomUUID().toString(),
        )
        MDC.put(CorrelacaoFilter.CHAVE_TRANSACAO, envelope.idMensagem)
    }

    private companion object {
        const val RESERVAR = "ReservarEstoque"
        const val CONSUMIR = "ConsumirReserva"
        const val LIBERAR = "LiberarReserva"
        const val ENTRADA = "RegistrarEntradaDeEstoque"
        const val APLICADO = "aplicado"
        const val INVALIDO = "invalido"
        const val ERRO = "erro"
        const val DESCONHECIDO = "desconhecido"
        val TIPOS = setOf(RESERVAR, CONSUMIR, LIBERAR, ENTRADA)
    }
}
