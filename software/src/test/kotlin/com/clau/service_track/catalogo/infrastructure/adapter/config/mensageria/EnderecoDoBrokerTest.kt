package com.clau.service_track.catalogo.infrastructure.adapter.config.mensageria

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.kafka.autoconfigure.KafkaProperties

class EnderecoDoBrokerTest {

    private fun kafkaCom(vararg enderecos: String) = KafkaProperties().apply {
        bootstrapServers = enderecos.toList()
    }

    @Test
    fun `mensageria ligada sem endereco de broker nao sobe`() {
        val erro = assertThrows<IllegalStateException> { MensageriaConfig(kafkaCom()) }

        assertTrue(
            assertNotNull(erro.message).contains("KAFKA_BOOTSTRAP_SERVERS"),
            "a falha precisa dizer qual variavel falta: ${erro.message}",
        )
    }

    @Test
    fun `endereco em branco conta como ausente, porque e nisso que o placeholder vazio resulta`() {
        assertThrows<IllegalStateException> { MensageriaConfig(kafkaCom("")) }
        assertThrows<IllegalStateException> { MensageriaConfig(kafkaCom("  ")) }
    }

    @Test
    fun `endereco inacessivel e aceito, porque broker fora do ar nao e configuracao errada`() {
        assertDoesNotThrow {
            MensageriaConfig(kafkaCom("broker-que-nao-existe:9092"))
        }
    }
}
