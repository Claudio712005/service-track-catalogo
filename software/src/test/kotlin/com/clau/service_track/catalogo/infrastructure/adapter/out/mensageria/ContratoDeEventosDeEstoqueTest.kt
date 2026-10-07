package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria

import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueCommand
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper

@SpringBootTest
class ContratoDeEventosDeEstoqueTest {

    @Autowired
    private lateinit var mapper: ObjectMapper

    private val esquema = EsquemasDeEstoque.carregar(EsquemasDeEstoque.EVENTOS)

    private val categoria = CategoriaDeInsumo.criar(
        codigo = "OLEO",
        nome = "Oleos e lubrificantes",
        unidadePadrao = UnidadeDeMedida.LITRO,
    )

    private val insumo = Insumo.criar(
        categoria = categoria,
        sku = "OLEO-5W30",
        nome = "Oleo sintetico 5W30",
        descricao = "Galao de oleo sintetico",
        custo = ValorMonetario.de("38.90"),
    )

    private val ordem = DomainId.gerar()

    private fun fabrica() = FabricaDeEventoDeEstoqueJson(mapper)

    private fun saldoComEntrada(disponivel: String): SaldoDeInsumo =
        SaldoDeInsumo.zerado(insumo.id, UnidadeDeMedida.LITRO).also {
            it.registrarEntrada(
                quantidade = BigDecimal(disponivel),
                custoUnitario = ValorMonetario.de("38.90"),
                origemTipo = OrigemDeMovimento.NOTA_ENTRADA,
                origemId = null,
                chaveIdempotencia = "carga-do-teste",
                registradoPor = null,
            )
        }

    private fun saldoReservado(): SaldoDeInsumo = saldoComEntrada("24").also {
        it.reservar(
            ordemServicoId = ordem,
            quantidade = BigDecimal("4"),
            expiraEm = LocalDateTime.now().plusMinutes(10),
            chaveIdempotencia = "$ordem:RESERVA_DE_INSUMOS:${insumo.id.value}",
        )
    }

    private fun exigirConforme(payload: String) {
        val violacoes = EsquemasDeEstoque.violacoes(esquema, payload)
        assertTrue(violacoes.isEmpty(), "evento fora do contrato: $violacoes\n$payload")
    }

    @Test
    fun `EstoqueReservado confere com o esquema publicado`() {
        val saldo = saldoComEntrada("24")
        val resultado = saldo.reservar(ordem, BigDecimal("4"), LocalDateTime.now().plusMinutes(10), "chave-1")

        exigirConforme(fabrica().estoqueReservado(insumo, resultado, saldo, TRACE).payload)
    }

    @Test
    fun `EstoqueConsumido confere com o esquema publicado`() {
        val saldo = saldoReservado()
        val resultado = saldo.consumirReserva(ordem, "chave-2")

        exigirConforme(fabrica().estoqueConsumido(insumo, resultado, saldo, TRACE).payload)
    }

    @Test
    fun `ReservaLiberada confere com o esquema publicado`() {
        val saldo = saldoReservado()
        val resultado = saldo.liberarReserva(ordem, "chave-3")

        exigirConforme(fabrica().reservaLiberada(insumo, resultado, saldo, TRACE).payload)
    }

    @Test
    fun `ReservaExpirada confere com o esquema publicado`() {
        val saldo = saldoReservado()
        val reserva = saldo.reservaAtivaDe(ordem)!!
        val resultado = saldo.expirarReserva(reserva, "chave-4")

        exigirConforme(fabrica().reservaExpirada(insumo, resultado, saldo, TRACE).payload)
    }

    @Test
    fun `ReservaRecusada confere com o esquema publicado`() {
        val comando = ReservarEstoqueCommand(
            insumoId = insumo.id,
            ordemServicoId = ordem,
            quantidade = BigDecimal("40"),
            expiraEm = LocalDateTime.now().plusMinutes(10),
            chaveIdempotencia = "chave-5",
            traceId = TRACE,
        )

        val evento = fabrica().reservaRecusada(insumo, comando, "solicitado 40 LITRO, disponivel 24 LITRO")

        exigirConforme(evento.payload)
    }

    @Test
    fun `evento sem campo obrigatorio e recusado, o que prova que a validacao roda`() {
        val saldo = saldoComEntrada("24")
        val resultado = saldo.reservar(ordem, BigDecimal("4"), null, "chave-6")
        val completo = fabrica().estoqueReservado(insumo, resultado, saldo, TRACE).payload

        val arvore = mapper.readTree(completo)
        (arvore["dados"] as tools.jackson.databind.node.ObjectNode).remove("saldoDisponivel")
        val mutilado = mapper.writeValueAsString(arvore)

        val violacoes = EsquemasDeEstoque.violacoes(esquema, mutilado)
        assertTrue(violacoes.isNotEmpty(), "validador aceitou evento sem saldoDisponivel")
        assertTrue(
            violacoes.any { it.contains("saldoDisponivel") },
            "violacao nao aponta o campo faltante: $violacoes",
        )
    }

    @Test
    fun `ConsumoRecusado esta no contrato antes de existir no codigo`() {
        val tipos = mapper
            .readTree(javaClass.getResourceAsStream(EsquemasDeEstoque.EVENTOS))["properties"]["tipo"]["enum"]
            .values()
            .map { it.asString() }

        assertEquals(6, tipos.size, "tipos publicados: $tipos")
        assertTrue("ConsumoRecusado" in tipos, "tipos publicados: $tipos")

        val evento = """
            {
              "idMensagem": "${DomainId.gerar().value}",
              "tipo": "ConsumoRecusado",
              "versao": 1,
              "ocorridoEm": "2026-10-06T14:05:00Z",
              "correlationId": "atendimento-88213",
              "traceId": "$TRACE",
              "dados": {
                "insumoId": "${insumo.id.value}",
                "sku": "OLEO-5W30",
                "ordemServicoId": "${ordem.value}",
                "motivo": "reserva inexistente ou ja consumida"
              }
            }
        """.trimIndent()

        exigirConforme(evento)
    }

    @Test
    fun `chave de particao do evento e a ordem de servico, que e o que preserva a ordem da saga`() {
        val saldo = saldoComEntrada("24")
        val resultado = saldo.reservar(ordem, BigDecimal("4"), null, "chave-7")

        assertEquals(
            ordem.value,
            fabrica().estoqueReservado(insumo, resultado, saldo, TRACE).chaveDeParticao,
        )
    }

    private companion object {
        const val TRACE = "4bf92f3577b34da6a3ce929d0e0e4736"
    }
}
