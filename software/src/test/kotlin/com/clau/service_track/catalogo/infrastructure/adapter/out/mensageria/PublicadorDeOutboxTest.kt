package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria

import com.clau.service_track.catalogo.infrastructure.adapter.config.mensageria.MensageriaProperties
import com.clau.service_track.catalogo.infrastructure.adapter.out.observabilidade.MetricasDeEstoque
import com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres.OutboxJpaRepository
import com.clau.service_track.catalogo.infrastructure.entity.postgres.OutboxEntity
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.apache.kafka.clients.producer.ProducerRecord
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.databind.ObjectMapper

class PublicadorDeOutboxTest {

    private val outbox: OutboxJpaRepository = mock()

    private val template: KafkaTemplate<String, String> = mock()

    private val propriedades = MensageriaProperties(
        habilitada = true,
        topicoDeEventos = "servicetrack.estoque.eventos.v1",
        loteDePublicacao = 50,
    )

    private val metricas = MetricasDeEstoque(SimpleMeterRegistry()) { 0L }

    private val publicador = PublicadorDeOutbox(outbox, template, propriedades, ObjectMapper(), metricas)

    private val insumo = UUID.randomUUID()

    private fun linha(
        tipo: String = "EstoqueReservado",
        trace: String? = "4bf92f3577b34da6a3ce929d0e0e4736",
        payload: String = """{"idMensagem":"m-1","tipo":"EstoqueReservado","correlationId":"atendimento-1"}""",
    ) = OutboxEntity(
        id = UUID.randomUUID(),
        agregadoTipo = "SaldoDeInsumo",
        agregadoId = insumo,
        chaveParticao = insumo.toString(),
        tipoEvento = tipo,
        versaoEvento = 1,
        payload = payload,
        traceId = trace,
        dataCriacao = OffsetDateTime.now(ZoneOffset.UTC),
    )

    private fun envioAceito() {
        whenever(template.send(any<ProducerRecord<String, String>>()))
            .doReturn(CompletableFuture.completedFuture(null))
    }

    private fun registroEnviado(): ProducerRecord<String, String> {
        val capturado = argumentCaptor<ProducerRecord<String, String>>()
        verify(template).send(capturado.capture())
        return capturado.firstValue
    }

    private fun cabecalho(registro: ProducerRecord<String, String>, nome: String): String? =
        registro.headers().lastHeader(nome)?.value()?.toString(StandardCharsets.UTF_8)

    @Test
    fun `outbox vazia nao chama o broker e devolve zero`() {
        whenever(outbox.reservarPendentes(any())).doReturn(emptyList())

        assertEquals(0, publicador.publicarPendentes())

        verify(template, never()).send(any<ProducerRecord<String, String>>())
    }

    @Test
    fun `reserva o lote do tamanho configurado, nao a tabela inteira`() {
        whenever(outbox.reservarPendentes(50)).doReturn(emptyList())

        publicador.publicarPendentes()

        verify(outbox).reservarPendentes(50)
    }

    @Test
    fun `cada linha vira um registro no topico de eventos, com tipo, versao e trace no cabecalho`() {
        val pendente = linha()
        whenever(outbox.reservarPendentes(any())).doReturn(listOf(pendente))
        envioAceito()

        assertEquals(1, publicador.publicarPendentes())

        val registro = registroEnviado()

        assertEquals("servicetrack.estoque.eventos.v1", registro.topic())
        assertEquals(insumo.toString(), registro.key(), "a chave de particao e o agregado, para ordem por insumo")
        assertEquals("EstoqueReservado", cabecalho(registro, "X-Tipo-Evento"))
        assertEquals("1", cabecalho(registro, "X-Versao-Evento"))
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", cabecalho(registro, "X-Trace-Id"))
        assertEquals("atendimento-1", cabecalho(registro, "X-Correlation-Id"))
    }

    @Test
    fun `linha publicada recebe data de publicacao em UTC`() {
        val pendente = linha()
        whenever(outbox.reservarPendentes(any())).doReturn(listOf(pendente))
        envioAceito()

        assertNull(pendente.dataPublicacao, "linha reservada ainda nao esta publicada")

        publicador.publicarPendentes()

        val publicacao = assertNotNull(pendente.dataPublicacao)
        assertEquals(0, publicacao.offset.totalSeconds, "a marca de publicacao e gravada em UTC")
    }

    @Test
    fun `sem traceId na linha o cabecalho de trace nao e inventado`() {
        val pendente = linha(trace = null)
        whenever(outbox.reservarPendentes(any())).doReturn(listOf(pendente))
        envioAceito()

        publicador.publicarPendentes()

        assertNull(cabecalho(registroEnviado(), "X-Trace-Id"))
    }

    @Test
    fun `payload que nao e json nao derruba a publicacao, so perde a correlacao`() {
        val pendente = linha(payload = "isto nao e json")
        whenever(outbox.reservarPendentes(any())).doReturn(listOf(pendente))
        envioAceito()

        assertEquals(1, publicador.publicarPendentes(), "payload ilegivel nao pode travar a fila")

        val registro = registroEnviado()
        assertNull(cabecalho(registro, "X-Correlation-Id"))
        assertEquals("EstoqueReservado", cabecalho(registro, "X-Tipo-Evento"))
    }

    @Test
    fun `payload sem correlationId publica sem o cabecalho de correlacao`() {
        val pendente = linha(payload = """{"idMensagem":"m-1","tipo":"EstoqueReservado"}""")
        whenever(outbox.reservarPendentes(any())).doReturn(listOf(pendente))
        envioAceito()

        publicador.publicarPendentes()

        assertNull(cabecalho(registroEnviado(), "X-Correlation-Id"))
    }

    @Test
    fun `broker fora do ar nao marca a linha como publicada`() {
        val pendente = linha()
        whenever(outbox.reservarPendentes(any())).doReturn(listOf(pendente))
        whenever(template.send(any<ProducerRecord<String, String>>()))
            .doReturn(CompletableFuture.failedFuture(IllegalStateException("broker fora do ar")))

        val erro = runCatching { publicador.publicarPendentes() }.exceptionOrNull()

        assertTrue(erro != null, "falha de envio tem de propagar, para a transacao nao marcar como publicado")
        assertNull(
            pendente.dataPublicacao,
            "linha que nao chegou ao broker continua pendente, e sai na proxima passagem",
        )
    }

    @Test
    fun `varias linhas sao publicadas na mesma passagem`() {
        val pendentes = listOf(linha(), linha(tipo = "ReservaLiberada"), linha(tipo = "EstoqueConsumido"))
        whenever(outbox.reservarPendentes(any())).doReturn(pendentes)
        envioAceito()

        assertEquals(3, publicador.publicarPendentes())

        verify(template, times(3)).send(any<ProducerRecord<String, String>>())
        assertTrue(pendentes.all { it.dataPublicacao != null })
    }
}
