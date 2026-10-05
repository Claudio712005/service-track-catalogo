package com.clau.service_track.catalogo.infrastructure.adapter.config.mensageria

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.MensagemInvalidaException
import com.clau.service_track.catalogo.infrastructure.adapter.out.observabilidade.MetricasDeEstoque
import com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres.OutboxJpaRepository
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
@EnableConfigurationProperties(MensageriaProperties::class)
@ConditionalOnProperty(prefix = "servicetrack.mensageria", name = ["habilitada"], havingValue = "true")
class MensageriaConfig {

    private val log = LoggerFactory.getLogger(MensageriaConfig::class.java)

    @Bean
    fun estoqueListenerContainerFactory(
        consumidores: ConsumerFactory<String, String>,
        propriedades: MensageriaProperties,
        deadLetterPublishingRecoverer: DeadLetterPublishingRecoverer,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val fabrica = ConcurrentKafkaListenerContainerFactory<String, String>()
        fabrica.setConsumerFactory(consumidores)
        fabrica.setConcurrency(propriedades.concorrencia)
        fabrica.setCommonErrorHandler(tratadorDeErro(propriedades, deadLetterPublishingRecoverer))
        fabrica.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        fabrica.containerProperties.isMissingTopicsFatal = false
        fabrica.containerProperties.isObservationEnabled = true
        return fabrica
    }

    @Bean
    fun metricasDeEstoque(registro: MeterRegistry, outbox: OutboxJpaRepository) =
        MetricasDeEstoque(registro) { outbox.countByDataPublicacaoIsNull() }

    @Bean
    fun deadLetterPublishingRecoverer(
        template: KafkaTemplate<String, String>,
        propriedades: MensageriaProperties,
    ): DeadLetterPublishingRecoverer = DeadLetterPublishingRecoverer(template) { registro, _ ->
        TopicPartition(registro.topic() + propriedades.sufixoDaDlt, PARTICAO_A_CARGO_DO_BROKER)
    }

    private fun tratadorDeErro(
        propriedades: MensageriaProperties,
        deadLetterPublishingRecoverer: DeadLetterPublishingRecoverer,
    ): DefaultErrorHandler {
        val espera = ExponentialBackOff().apply {
            initialInterval = propriedades.esperaInicial.toMillis()
            multiplier = propriedades.multiplicadorDaEspera
            maxInterval = propriedades.esperaMaxima.toMillis()
            maxAttempts = propriedades.tentativas.toLong()
        }

        return DefaultErrorHandler(deadLetterPublishingRecoverer, espera).apply {
            addNotRetryableExceptions(
                MensagemInvalidaException::class.java,
                DomainException::class.java,
                RecursoNaoEncontradoException::class.java,
            )
            setRetryListeners({ registro, excecao, tentativa ->
                log.warn(
                    "falha ao processar comando, tentativa={} topico={} particao={} offset={} causa={}",
                    tentativa, registro.topic(), registro.partition(), registro.offset(),
                    excecao?.cause?.javaClass?.simpleName ?: excecao?.javaClass?.simpleName,
                )
            })
        }
    }

    private companion object {
        const val PARTICAO_A_CARGO_DO_BROKER = -1
    }
}
