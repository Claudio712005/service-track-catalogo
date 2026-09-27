package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

import com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres.OutboxJpaRepository
import com.clau.service_track.catalogo.infrastructure.entity.postgres.OutboxEntity
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(prefix = "servicetrack.mensageria", name = ["habilitada"], havingValue = "true")
class PublicadorDeOutbox(
    private val outbox: OutboxJpaRepository,
    private val template: KafkaTemplate<String, String>,
    private val propriedades: PropriedadesDeMensageria,
    private val mapper: ObjectMapper,
    private val metricas: MetricasDeEstoque,
) {

    private val log = LoggerFactory.getLogger(PublicadorDeOutbox::class.java)

    @Transactional
    fun publicarPendentes(): Int {
        val pendentes = outbox.reservarPendentes(propriedades.loteDePublicacao)
        if (pendentes.isEmpty()) return 0

        pendentes.forEach(::enviar)
        metricas.eventosPublicados(pendentes.size)
        log.info("eventos publicados quantidade={} topico={}", pendentes.size, propriedades.topicoDeEventos)
        return pendentes.size
    }

    private fun enviar(linha: OutboxEntity) {
        val registro = ProducerRecord<String, String>(
            propriedades.topicoDeEventos,
            linha.chaveParticao,
            linha.payload,
        )
        registro.headers().add(cabecalho(CABECALHO_TIPO, linha.tipoEvento))
        registro.headers().add(cabecalho(CABECALHO_VERSAO, linha.versaoEvento.toString()))
        linha.traceId?.let { registro.headers().add(cabecalho(CABECALHO_TRACE, it)) }
        correlacaoDe(linha)?.let { registro.headers().add(cabecalho(CABECALHO_CORRELACAO, it)) }

        template.send(registro).get(ESPERA_DO_ENVIO_EM_SEGUNDOS, TimeUnit.SECONDS)
        linha.dataPublicacao = OffsetDateTime.now(ZoneOffset.UTC)
    }

    private fun correlacaoDe(linha: OutboxEntity): String? = runCatching {
        mapper.readValue(linha.payload, CorrelacaoDoEnvelope::class.java).correlationId
    }.getOrNull()

    private fun cabecalho(nome: String, valor: String) =
        RecordHeader(nome, valor.toByteArray(StandardCharsets.UTF_8))

    private data class CorrelacaoDoEnvelope(val correlationId: String? = null)

    private companion object {
        const val CABECALHO_TIPO = "X-Tipo-Evento"
        const val CABECALHO_VERSAO = "X-Versao-Evento"
        const val CABECALHO_TRACE = "X-Trace-Id"
        const val CABECALHO_CORRELACAO = "X-Correlation-Id"
        const val ESPERA_DO_ENVIO_EM_SEGUNDOS = 15L
    }
}
