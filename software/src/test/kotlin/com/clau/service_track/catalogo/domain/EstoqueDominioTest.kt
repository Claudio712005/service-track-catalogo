package com.clau.service_track.catalogo.domain

import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.model.SaldoInsuficienteException
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import com.clau.service_track.catalogo.domain.vo.TipoDeMovimento
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EstoqueDominioTest {

    private val insumo = DomainId.gerar()
    private val ordem = DomainId.gerar()

    private fun saldoDeLitros(disponivel: String = "0") = SaldoDeInsumo
        .zerado(insumo, UnidadeDeMedida.LITRO)
        .also {
            if (BigDecimal(disponivel) > BigDecimal.ZERO) {
                it.registrarEntrada(
                    quantidade = BigDecimal(disponivel),
                    custoUnitario = ValorMonetario.de("38.90"),
                    origemTipo = OrigemDeMovimento.NOTA_ENTRADA,
                    origemId = null,
                    chaveIdempotencia = "carga-inicial",
                    registradoPor = null,
                )
            }
        }

    @Test
    fun `entrada soma ao disponivel e grava o saldo apos o movimento`() {
        val saldo = saldoDeLitros()

        val movimento = saldo.registrarEntrada(
            quantidade = BigDecimal("24"),
            custoUnitario = ValorMonetario.de("38.90"),
            origemTipo = OrigemDeMovimento.NOTA_ENTRADA,
            origemId = null,
            chaveIdempotencia = "nf-1",
            registradoPor = null,
        )

        assertEquals(0, BigDecimal("24").compareTo(saldo.quantidadeDisponivel))
        assertEquals(TipoDeMovimento.ENTRADA, movimento.tipo)
        assertEquals(0, BigDecimal("24").compareTo(movimento.saldoDisponivelApos))
    }

    @Test
    fun `reserva move do disponivel para o reservado sem alterar o total`() {
        val saldo = saldoDeLitros("20")

        val resultado = saldo.reservar(ordem, BigDecimal("6"), null, "cmd-1")

        assertEquals(0, BigDecimal("14").compareTo(saldo.quantidadeDisponivel))
        assertEquals(0, BigDecimal("6").compareTo(saldo.quantidadeReservada))
        assertEquals(StatusDeReserva.ATIVA, resultado.reserva.status)
        assertEquals(TipoDeMovimento.RESERVA, resultado.movimento.tipo)
    }

    @Test
    fun `reserva acima do disponivel e recusada com solicitado e disponivel no erro`() {
        val saldo = saldoDeLitros("5")

        val erro = assertFailsWith<SaldoInsuficienteException> {
            saldo.reservar(ordem, BigDecimal("9"), null, "cmd-1")
        }

        assertEquals(0, BigDecimal("9").compareTo(erro.solicitado))
        assertEquals(0, BigDecimal("5").compareTo(erro.disponivel))
        assertEquals(0, BigDecimal("5").compareTo(saldo.quantidadeDisponivel))
        assertEquals(0, BigDecimal.ZERO.compareTo(saldo.quantidadeReservada))
    }

    @Test
    fun `a mesma ordem nao reserva o mesmo insumo duas vezes`() {
        val saldo = saldoDeLitros("20")
        saldo.reservar(ordem, BigDecimal("6"), null, "cmd-1")

        assertFailsWith<ConflitoDeEstadoException> { saldo.reservar(ordem, BigDecimal("2"), null, "cmd-2") }
        assertEquals(0, BigDecimal("6").compareTo(saldo.quantidadeReservada))
    }

    @Test
    fun `consumo baixa o reservado e nao devolve ao disponivel`() {
        val saldo = saldoDeLitros("20")
        saldo.reservar(ordem, BigDecimal("6"), null, "cmd-1")

        val resultado = saldo.consumirReserva(ordem, "cmd-2")

        assertEquals(0, BigDecimal("14").compareTo(saldo.quantidadeDisponivel))
        assertEquals(0, BigDecimal.ZERO.compareTo(saldo.quantidadeReservada))
        assertEquals(StatusDeReserva.CONSUMIDA, resultado.reserva.status)
        assertEquals(TipoDeMovimento.CONSUMO_RESERVA, resultado.movimento.tipo)
        assertTrue(saldo.reservasAtivas.isEmpty())
    }

    @Test
    fun `liberacao devolve ao disponivel e fecha o ciclo de compensacao`() {
        val saldo = saldoDeLitros("20")
        saldo.reservar(ordem, BigDecimal("6"), null, "cmd-1")

        val resultado = saldo.liberarReserva(ordem, "cmd-2")

        assertEquals(0, BigDecimal("20").compareTo(saldo.quantidadeDisponivel))
        assertEquals(0, BigDecimal.ZERO.compareTo(saldo.quantidadeReservada))
        assertEquals(StatusDeReserva.LIBERADA, resultado.reserva.status)
    }

    @Test
    fun `consumir sem reserva ativa e conflito, nao erro de dado`() {
        val saldo = saldoDeLitros("20")

        assertFailsWith<ConflitoDeEstadoException> { saldo.consumirReserva(ordem, "cmd-1") }
    }

    @Test
    fun `reserva vencida expira e devolve a quantidade`() {
        val saldo = saldoDeLitros("20")
        val reserva = saldo.reservar(ordem, BigDecimal("6"), LocalDateTime.now().minusMinutes(1), "cmd-1").reserva

        assertTrue(reserva.expirada(LocalDateTime.now()))

        val resultado = saldo.expirarReserva(reserva, "expiracao:${reserva.id.value}")

        assertEquals(StatusDeReserva.EXPIRADA, resultado.reserva.status)
        assertEquals(0, BigDecimal("20").compareTo(saldo.quantidadeDisponivel))
    }

    @Test
    fun `reserva ja encerrada nao expira de novo`() {
        val saldo = saldoDeLitros("20")
        val reserva = saldo.reservar(ordem, BigDecimal("6"), LocalDateTime.now().minusMinutes(1), "cmd-1").reserva
        saldo.consumirReserva(ordem, "cmd-2")

        assertFailsWith<ConflitoDeEstadoException> { saldo.expirarReserva(reserva, "expiracao:x") }
    }

    @Test
    fun `unidade inteira recusa quantidade fracionaria`() {
        val saldo = SaldoDeInsumo.zerado(insumo, UnidadeDeMedida.UNIDADE)

        val erro = assertFailsWith<DomainException> {
            saldo.registrarEntrada(BigDecimal("2.5"), null, OrigemDeMovimento.NOTA_ENTRADA, null, "nf-1", null)
        }

        assertTrue(erro.message!!.contains("não admite fração"))
    }

    @Test
    fun `estoque minimo sinaliza reposicao sem bloquear operacao`() {
        val saldo = saldoDeLitros("20")
        saldo.definirEstoqueMinimo(BigDecimal("18"))

        assertFalse(saldo.abaixoDoMinimo)

        saldo.reservar(ordem, BigDecimal("6"), null, "cmd-1")

        assertTrue(saldo.abaixoDoMinimo)
        assertEquals(0, BigDecimal("14").compareTo(saldo.quantidadeDisponivel))
    }

    @Test
    fun `quantidade zero ou negativa e recusada`() {
        val saldo = saldoDeLitros("20")

        assertFailsWith<DomainException> {
            saldo.registrarEntrada(BigDecimal.ZERO, null, OrigemDeMovimento.NOTA_ENTRADA, null, "nf-1", null)
        }
        assertFailsWith<DomainException> { saldo.reservar(ordem, BigDecimal("-1"), null, "cmd-1") }
    }
}
