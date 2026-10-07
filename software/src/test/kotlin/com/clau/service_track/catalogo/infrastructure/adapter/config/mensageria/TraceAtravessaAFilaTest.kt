package com.clau.service_track.catalogo.infrastructure.adapter.config.mensageria

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.micrometer.tracing.Tracer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.stereotype.Component

@SpringBootTest(
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=teste-do-trace",
        "spring.kafka.template.observation-enabled=true",
        "management.tracing.sampling.probability=1.0",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.metrics.export.enabled=false",
        "servicetrack.mensageria.habilitada=false",
    ],
)
@EmbeddedKafka(topics = [TraceAtravessaAFilaTest.TOPICO], partitions = 1)
@AutoConfigureTracing
class TraceAtravessaAFilaTest {

    @Autowired
    private lateinit var template: KafkaTemplate<String, String>

    @Autowired
    private lateinit var tracer: Tracer

    @Autowired
    private lateinit var consumidor: ConsumidorDeTeste

    @Autowired
    private lateinit var produtores: org.springframework.kafka.core.ProducerFactory<String, String>


    @Test
    fun `o produtor injeta traceparent e o consumidor entra no mesmo trace`() {
        val marca = UUID.randomUUID().toString()
        val span = tracer.nextSpan().name("origem-do-fluxo").start()
        val traceIdDaOrigem = span.context().traceId()

        tracer.withSpan(span).use {
            template.send(ProducerRecord<String, String>(TOPICO, marca, marca))
                .get(ESPERA_DE_ENVIO, TimeUnit.SECONDS)
        }
        span.end()

        val recebida = consumidor.esperarPor(marca)

        assertTrue(
            recebida.cabecalhos.any { it.startsWith("traceparent=00-$traceIdDaOrigem-") },
            "o produtor nao injetou traceparent com o traceId da origem: ${recebida.cabecalhos}",
        )
        assertEquals(
            traceIdDaOrigem,
            recebida.traceId,
            "o consumidor nao entrou no trace da origem: o rastro morre na fila",
        )
    }

    @Test
    fun `o consumidor adota o traceparent de um produtor externo`() {
        val traceIdExterno = "4bf92f3577b34da6a3ce929d0e0e4736"
        val marca = UUID.randomUUID().toString()
        val registro = ProducerRecord<String, String>(TOPICO, marca, marca)
        registro.headers().add(
            RecordHeader("traceparent", "00-$traceIdExterno-00f067aa0ba902b7-01".toByteArray()),
        )

        val produtorExterno = KafkaTemplate(produtores).apply { setObservationEnabled(false) }
        produtorExterno.send(registro).get(ESPERA_DE_ENVIO, TimeUnit.SECONDS)

        val recebida = consumidor.esperarPor(marca)

        assertTrue(
            recebida.cabecalhos.any { it == "traceparent=00-$traceIdExterno-00f067aa0ba902b7-01" },
            "o cabecalho do produtor externo nao chegou intacto: ${recebida.cabecalhos}",
        )
        assertEquals(
            traceIdExterno,
            recebida.traceId,
            "o consumidor ignorou o traceparent de quem publicou",
        )
    }

    @Test
    fun `publicacao fora do span de origem abre trace novo, e e isso que a outbox faz`() {
        val marca = UUID.randomUUID().toString()
        val traceIdDaOrigem = tracer.nextSpan().name("requisicao-que-gerou-o-evento").start()
            .also { it.end() }
            .context()
            .traceId()

        template.send(ProducerRecord<String, String>(TOPICO, marca, marca))
            .get(ESPERA_DE_ENVIO, TimeUnit.SECONDS)

        val recebida = consumidor.esperarPor(marca)
        val traceparent = recebida.cabecalhos.first { it.startsWith("traceparent=") }

        assertTrue(
            traceparent.contains("-"),
            "sem traceparent nenhum a publicacao assincrona nem apareceria no rastreio",
        )
        assertTrue(
            !traceparent.contains(traceIdDaOrigem),
            "este teste documenta a lacuna: a publicacao da outbox roda fora do span de origem, " +
                "entao o traceparent leva o trace da publicacao, nao o da requisicao. " +
                "traceparent=$traceparent origem=$traceIdDaOrigem",
        )
        assertEquals(
            recebida.traceId,
            traceparent.removePrefix("traceparent=").split("-")[1],
            "o consumidor entra no trace da publicacao, que e um trace vizinho e nao o da origem",
        )
    }

    @TestConfiguration
    class Config {

        @Bean
        fun fabricaDeTeste(
            consumidores: ConsumerFactory<String, String>,
        ): ConcurrentKafkaListenerContainerFactory<String, String> =
            ConcurrentKafkaListenerContainerFactory<String, String>().apply {
                setConsumerFactory(consumidores)
                containerProperties.isObservationEnabled = true
            }

        @Bean
        fun consumidorDeTeste() = ConsumidorDeTeste()
    }

    class MensagemRecebida(
        val traceId: String?,
        val cabecalhos: List<String>,
    )

    @Component
    class ConsumidorDeTeste {

        private val recebidas = ConcurrentHashMap<String, MensagemRecebida>()

        fun esperarPor(marca: String): MensagemRecebida {
            val limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(ESPERA_DE_CONSUMO)
            while (System.nanoTime() < limite) {
                recebidas[marca]?.let { return it }
                Thread.sleep(50)
            }
            throw AssertionError("a mensagem $marca nao foi consumida em ${ESPERA_DE_CONSUMO}s")
        }

        @KafkaListener(topics = [TOPICO], containerFactory = "fabricaDeTeste")
        fun consumir(registro: ConsumerRecord<String, String>) {
            recebidas[registro.value()] = MensagemRecebida(
                traceId = MDC.get("traceId"),
                cabecalhos = registro.headers().map { "${it.key()}=${String(it.value())}" },
            )
        }
    }

    companion object {
        const val TOPICO = "servicetrack.teste.trace.v1"
        private const val ESPERA_DE_ENVIO = 10L
        private const val ESPERA_DE_CONSUMO = 30L
    }
}
