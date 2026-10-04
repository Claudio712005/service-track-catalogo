package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria

import com.clau.service_track.catalogo.EstoqueRepositoryMemoriaAdapter
import com.clau.service_track.catalogo.InsumoRepositoryMemoriaAdapter
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsumirReservaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.LiberarReservaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.RegistrarEntradaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueUseCase
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.infrastructure.adapter.out.observabilidade.MetricasDeEstoque
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper

@SpringBootTest
class ConsumidorDeComandosDeEstoqueTest {

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var reservar: ReservarEstoqueUseCase

    @Autowired
    private lateinit var consumirReserva: ConsumirReservaUseCase

    @Autowired
    private lateinit var liberar: LiberarReservaUseCase

    @Autowired
    private lateinit var registrarEntrada: RegistrarEntradaUseCase

    @Autowired
    private lateinit var estoque: EstoqueRepositoryMemoriaAdapter

    private lateinit var consumidor: ConsumidorDeComandosDeEstoque

    private val ordem = UUID.randomUUID().toString()

    @BeforeEach
    fun preparar() {
        estoque.reiniciar()
        consumidor = ConsumidorDeComandosDeEstoque(
            leitor = LeitorDeEnvelope(mapper),
            reservar = reservar,
            consumir = consumirReserva,
            liberar = liberar,
            registrarEntrada = registrarEntrada,
            metricas = MetricasDeEstoque(SimpleMeterRegistry()) { 0L },
        )
    }

    @Test
    fun `entrada e reserva atravessam a fila e chegam ao saldo`() {
        consumidor.consumir(mensagem("RegistrarEntradaDeEstoque", entrada("40")))
        consumidor.consumir(mensagem("ReservarEstoque", reservaDe("6")))

        val saldo = assertNotNull(estoque.buscarSaldo(DomainId.de(OLEO)))
        assertEquals(0, BigDecimal("34").compareTo(saldo.quantidadeDisponivel))
        assertEquals(0, BigDecimal("6").compareTo(saldo.quantidadeReservada))
        assertEquals("EstoqueReservado", estoque.eventos.last().tipoEvento)
        assertEquals(ordem, estoque.eventos.last().chaveDeParticao)
    }

    @Test
    fun `mensagem repetida nao aplica o efeito duas vezes`() {
        consumidor.consumir(mensagem("RegistrarEntradaDeEstoque", entrada("40")))
        val repetida = mensagem("ReservarEstoque", reservaDe("6"))

        consumidor.consumir(repetida)
        consumidor.consumir(repetida)

        val saldo = assertNotNull(estoque.buscarSaldo(DomainId.de(OLEO)))
        assertEquals(0, BigDecimal("6").compareTo(saldo.quantidadeReservada))
        assertEquals(1, estoque.eventos.count { it.tipoEvento == "EstoqueReservado" })
    }

    @Test
    fun `mesma chave em comandos diferentes aplica os dois efeitos`() {
        val chave = UUID.randomUUID().toString()

        consumidor.consumir(mensagem("RegistrarEntradaDeEstoque", entrada("40"), chave))
        consumidor.consumir(mensagem("ReservarEstoque", reservaDe("6"), chave))

        val saldo = assertNotNull(estoque.buscarSaldo(DomainId.de(OLEO)))
        assertEquals(0, BigDecimal("34").compareTo(saldo.quantidadeDisponivel))
        assertEquals(0, BigDecimal("6").compareTo(saldo.quantidadeReservada))
    }

    @Test
    fun `saldo insuficiente publica recusa em vez de estourar`() {
        consumidor.consumir(mensagem("RegistrarEntradaDeEstoque", entrada("2")))
        consumidor.consumir(mensagem("ReservarEstoque", reservaDe("9")))

        val recusa = assertNotNull(estoque.eventos.lastOrNull())
        assertEquals("ReservaRecusada", recusa.tipoEvento)
        assertTrue(recusa.payload.contains("Saldo insuficiente"))

        val saldo = assertNotNull(estoque.buscarSaldo(DomainId.de(OLEO)))
        assertEquals(0, BigDecimal.ZERO.compareTo(saldo.quantidadeReservada))
    }

    @Test
    fun `ciclo completo reserva e consumo deixa o disponivel baixado`() {
        consumidor.consumir(mensagem("RegistrarEntradaDeEstoque", entrada("40")))
        consumidor.consumir(mensagem("ReservarEstoque", reservaDe("6")))
        consumidor.consumir(mensagem("ConsumirReserva", reserva()))

        val saldo = assertNotNull(estoque.buscarSaldo(DomainId.de(OLEO)))
        assertEquals(0, BigDecimal("34").compareTo(saldo.quantidadeDisponivel))
        assertEquals(0, BigDecimal.ZERO.compareTo(saldo.quantidadeReservada))
        assertEquals("EstoqueConsumido", estoque.eventos.last().tipoEvento)
    }

    @Test
    fun `liberacao devolve o reservado ao disponivel`() {
        consumidor.consumir(mensagem("RegistrarEntradaDeEstoque", entrada("40")))
        consumidor.consumir(mensagem("ReservarEstoque", reservaDe("6")))
        consumidor.consumir(mensagem("LiberarReserva", reserva()))

        val saldo = assertNotNull(estoque.buscarSaldo(DomainId.de(OLEO)))
        assertEquals(0, BigDecimal("40").compareTo(saldo.quantidadeDisponivel))
        assertEquals("ReservaLiberada", estoque.eventos.last().tipoEvento)
    }

    @Test
    fun `comando desconhecido nao e retentado, vai para a DLT`() {
        val erro = assertFailsWith<MensagemInvalidaException> {
            consumidor.consumir(mensagem("ReprecificarInsumo", reserva()))
        }

        assertTrue(erro.message!!.contains("não é reconhecido"))
        assertNull(estoque.buscarSaldo(DomainId.de(OLEO)))
    }

    @Test
    fun `corpo fora do contrato do comando e mensagem invalida`() {
        val erro = assertFailsWith<MensagemInvalidaException> {
            consumidor.consumir(mensagem("ReservarEstoque", """{"insumoId":"$OLEO"}"""))
        }

        assertTrue(erro.message!!.contains("não correspondem ao contrato"))
    }

    @Test
    fun `envelope sem identificador de mensagem e recusado antes de tocar o dominio`() {
        val corpo = """
        {"tipo":"ReservarEstoque","versao":1,"ocorridoEm":"2026-09-27T10:00:00Z","dados":${reservaDe("6")}}
        """
        val erro = assertFailsWith<MensagemInvalidaException> {
            consumidor.consumir(ConsumerRecord(TOPICO, 0, 0L, ordem, corpo))
        }

        assertTrue(erro.message!!.contains("não pôde ser lido"))
    }

    @Test
    fun `corpo que nao e json vai para a DLT sem retentativa`() {
        assertFailsWith<MensagemInvalidaException> {
            consumidor.consumir(ConsumerRecord(TOPICO, 0, 0L, ordem, "isto nao e json"))
        }
    }

    private fun mensagem(
        tipo: String,
        dados: String,
        idMensagem: String = UUID.randomUUID().toString(),
    ): ConsumerRecord<String, String> {
        val corpo = """
        {
          "idMensagem": "$idMensagem",
          "tipo": "$tipo",
          "versao": 1,
          "ocorridoEm": "2026-09-27T10:00:00Z",
          "correlationId": "teste-correlacao",
          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
          "dados": $dados
        }
        """
        return ConsumerRecord(TOPICO, 0, 0L, ordem, corpo)
    }

    private fun entrada(quantidade: String) = """
        {"insumoId":"$OLEO","quantidade":$quantidade,"custoUnitario":38.90,"origemTipo":"NOTA_ENTRADA"}
    """

    private fun reservaDe(quantidade: String) = """
        {"insumoId":"$OLEO","ordemServicoId":"$ordem","quantidade":$quantidade}
    """

    private fun reserva() = """{"insumoId":"$OLEO","ordemServicoId":"$ordem"}"""

    private companion object {
        const val TOPICO = "servicetrack.estoque.comandos.v1"
        const val OLEO = InsumoRepositoryMemoriaAdapter.OLEO_5W30
    }
}
