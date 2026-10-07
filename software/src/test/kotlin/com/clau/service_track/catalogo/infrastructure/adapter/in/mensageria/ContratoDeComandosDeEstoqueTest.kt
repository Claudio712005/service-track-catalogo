package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria

import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.dto.DadosDeEntradaDeEstoque
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.dto.DadosDeReservaEmAndamento
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.dto.DadosDeReservarEstoque
import com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.EsquemasDeEstoque
import com.clau.service_track.catalogo.infrastructure.entity.postgres.InboxEntity
import jakarta.persistence.Column
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper

@SpringBootTest
class ContratoDeComandosDeEstoqueTest {

    @Autowired
    private lateinit var mapper: ObjectMapper

    private val esquema = EsquemasDeEstoque.carregar(EsquemasDeEstoque.COMANDOS)

    private val insumo = UUID.randomUUID().toString()
    private val ordem = UUID.randomUUID().toString()

    private fun leitor() = LeitorDeEnvelope(mapper)

    private fun registro(corpo: String) =
        ConsumerRecord("servicetrack.estoque.comandos.v1", 0, 0L, ordem, corpo)

    private fun envelope(tipo: String, dados: String, idMensagem: String = UUID.randomUUID().toString()) = """
        {
          "idMensagem": "$idMensagem",
          "tipo": "$tipo",
          "versao": 1,
          "ocorridoEm": "2026-10-06T14:05:00Z",
          "correlationId": "atendimento-88213",
          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
          "dados": $dados
        }
    """.trimIndent()

    private fun exigirConforme(corpo: String) {
        val violacoes = EsquemasDeEstoque.violacoes(esquema, corpo)
        assertTrue(violacoes.isEmpty(), "comando fora do contrato: $violacoes\n$corpo")
    }

    private val reservar = """
        {
          "insumoId": "$insumo",
          "ordemServicoId": "$ordem",
          "quantidade": 4,
          "expiraEm": "2026-10-06T14:15:00Z"
        }
    """.trimIndent()

    private val reservaEmAndamento = """
        { "insumoId": "$insumo", "ordemServicoId": "$ordem" }
    """.trimIndent()

    private val entrada = """
        {
          "insumoId": "$insumo",
          "quantidade": 24,
          "custoUnitario": 38.90,
          "origemTipo": "NOTA_ENTRADA"
        }
    """.trimIndent()

    @Test
    fun `ReservarEstoque conforme o esquema e aceito pelo consumidor`() {
        val corpo = envelope("ReservarEstoque", reservar)
        exigirConforme(corpo)

        val lido = leitor().ler(registro(corpo))
        val dados = leitor().dados(lido, DadosDeReservarEstoque::class.java)

        assertEquals(insumo, dados.insumoId)
        assertEquals(ordem, dados.ordemServicoId)
        assertEquals(0, dados.quantidade.compareTo(java.math.BigDecimal("4")))
        assertNotNull(dados.expiraEm)
    }

    @Test
    fun `ConsumirReserva e LiberarReserva compartilham o corpo e sao aceitos pelo consumidor`() {
        listOf("ConsumirReserva", "LiberarReserva").forEach { tipo ->
            val corpo = envelope(tipo, reservaEmAndamento)
            exigirConforme(corpo)

            val dados = leitor().dados(leitor().ler(registro(corpo)), DadosDeReservaEmAndamento::class.java)
            assertEquals(ordem, dados.ordemServicoId, tipo)
        }
    }

    @Test
    fun `RegistrarEntradaDeEstoque conforme o esquema e aceito pelo consumidor`() {
        val corpo = envelope("RegistrarEntradaDeEstoque", entrada)
        exigirConforme(corpo)

        val dados = leitor().dados(leitor().ler(registro(corpo)), DadosDeEntradaDeEstoque::class.java)
        assertEquals(insumo, dados.insumoId)
    }

    @Test
    fun `chave da saga passa pelo esquema e cabe no INBOX`() {
        val chave = "$ordem:LIBERACAO_DE_INSUMOS:$insumo"
        assertEquals(94, chave.length, "a etapa mais longa da saga e o pior caso do orcamento")

        exigirConforme(envelope("ReservarEstoque", reservar, idMensagem = chave))

        val tetoDoInbox = InboxEntity::class.java
            .getDeclaredField("id")
            .getAnnotation(Column::class.java)
            .length

        assertTrue(
            "LiberarReserva:$chave".length <= tetoDoInbox,
            "chave de ${chave.length} com prefixo do tipo nao cabe em INBOX.ID($tetoDoInbox)",
        )
    }

    @Test
    fun `nenhum tipo do esquema estoura o INBOX no limite do idMensagem`() {
        val raiz = mapper.readTree(javaClass.getResourceAsStream(EsquemasDeEstoque.COMANDOS))
        val teto = raiz["properties"]["idMensagem"]["maxLength"].asInt()
        val tipos = raiz["properties"]["tipo"]["enum"].values().map { it.asString() } + "ExpirarReserva"

        val tetoDoInbox = InboxEntity::class.java
            .getDeclaredField("id")
            .getAnnotation(Column::class.java)
            .length

        tipos.forEach { tipo ->
            assertTrue(
                tipo.length + 1 + teto <= tetoDoInbox,
                "tipo $tipo com idMensagem de $teto estoura INBOX.ID($tetoDoInbox)",
            )
        }
    }

    @Test
    fun `esquema recusa reserva sem prazo, e o consumidor aceita de proposito`() {
        val semPrazo = """
            { "insumoId": "$insumo", "ordemServicoId": "$ordem", "quantidade": 4 }
        """.trimIndent()
        val corpo = envelope("ReservarEstoque", semPrazo)

        assertTrue(
            EsquemasDeEstoque.violacoes(esquema, corpo).isNotEmpty(),
            "expiraEm e obrigatorio no contrato: o relogio e do orquestrador",
        )

        val dados = leitor().dados(leitor().ler(registro(corpo)), DadosDeReservarEstoque::class.java)
        assertEquals(null, dados.expiraEm)
    }

    @Test
    fun `esquema recusa quantidade nao positiva e tipo desconhecido`() {
        val quantidadeZero = """
            {
              "insumoId": "$insumo",
              "ordemServicoId": "$ordem",
              "quantidade": 0,
              "expiraEm": "2026-10-06T14:15:00Z"
            }
        """.trimIndent()

        assertTrue(EsquemasDeEstoque.violacoes(esquema, envelope("ReservarEstoque", quantidadeZero)).isNotEmpty())
        assertTrue(EsquemasDeEstoque.violacoes(esquema, envelope("DescartarEstoque", reservar)).isNotEmpty())
    }

    @Test
    fun `campo desconhecido em dados passa pelo esquema e pelo consumidor`() {
        val comExtra = """
            {
              "insumoId": "$insumo",
              "ordemServicoId": "$ordem",
              "quantidade": 4,
              "expiraEm": "2026-10-06T14:15:00Z",
              "prioridadeDaSaga": "ALTA"
            }
        """.trimIndent()
        val corpo = envelope("ReservarEstoque", comExtra)

        exigirConforme(corpo)

        val dados = leitor().dados(leitor().ler(registro(corpo)), DadosDeReservarEstoque::class.java)
        assertEquals(insumo, dados.insumoId)
    }

    @Test
    fun `traceparent do cabecalho vence o traceId do envelope`() {
        val corpo = envelope("ConsumirReserva", reservaEmAndamento)
        exigirConforme(corpo)

        val registro = registro(corpo)
        registro.headers().add(
            LeitorDeEnvelope.CABECALHO_TRACE,
            "00-9f1c7a025f8e4e2a9a710c3d5b6e8f10-00f067aa0ba902b7-01".toByteArray(),
        )

        assertEquals("9f1c7a025f8e4e2a9a710c3d5b6e8f10", leitor().ler(registro).traceId)
    }

    @Test
    fun `traceparent do cabecalho e lido quando o envelope nao tem traceId`() {
        val semTrace = """
            {
              "idMensagem": "${UUID.randomUUID()}",
              "tipo": "ConsumirReserva",
              "versao": 1,
              "ocorridoEm": "2026-10-06T14:05:00Z",
              "dados": $reservaEmAndamento
            }
        """.trimIndent()

        exigirConforme(semTrace)

        val registro = registro(semTrace)
        registro.headers().add(
            LeitorDeEnvelope.CABECALHO_TRACE,
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01".toByteArray(),
        )

        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", leitor().ler(registro).traceId)
    }
}
