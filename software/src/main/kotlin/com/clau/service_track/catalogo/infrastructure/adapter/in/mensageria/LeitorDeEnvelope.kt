package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria

import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.dto.EnvelopeDeMensagem
import java.nio.charset.StandardCharsets
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper

@Component
class LeitorDeEnvelope(
    private val mapper: ObjectMapper,
) {

    fun ler(registro: ConsumerRecord<String, String>): EnvelopeDeMensagem {
        val corpo = registro.value()
            ?: throw MensagemInvalidaException("Mensagem sem corpo em ${registro.topic()}-${registro.partition()}")

        val envelope = try {
            mapper.readValue(corpo, EnvelopeDeMensagem::class.java)
        } catch (e: JacksonException) {
            throw MensagemInvalidaException("Envelope da mensagem não pôde ser lido: ${e.originalMessage}", e)
        }

        return envelope.copy(
            correlationId = envelope.correlationId ?: cabecalho(registro, CABECALHO_CORRELACAO),
            traceId = envelope.traceId ?: traceDoCabecalho(registro),
        )
    }

    fun <T : Any> dados(envelope: EnvelopeDeMensagem, tipo: Class<T>): T = try {
        mapper.treeToValue(envelope.dados, tipo)
    } catch (e: JacksonException) {
        throw MensagemInvalidaException(
            "Dados do comando ${envelope.tipo} não correspondem ao contrato: ${e.originalMessage}",
            e,
        )
    }

    private fun cabecalho(registro: ConsumerRecord<String, String>, nome: String): String? = registro.headers()
        .lastHeader(nome)
        ?.value()
        ?.toString(StandardCharsets.UTF_8)
        ?.takeIf { it.isNotBlank() }

    private fun traceDoCabecalho(registro: ConsumerRecord<String, String>): String? =
        cabecalho(registro, CABECALHO_TRACE)
            ?.split('-')
            ?.takeIf { it.size >= 3 }
            ?.get(1)

    companion object {
        const val CABECALHO_CORRELACAO = "X-Correlation-Id"
        const val CABECALHO_TRACE = "traceparent"
    }
}
