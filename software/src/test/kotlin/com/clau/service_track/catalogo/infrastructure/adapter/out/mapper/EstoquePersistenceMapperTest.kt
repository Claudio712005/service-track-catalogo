package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.application.port.out.repository.EventoParaPublicar
import com.clau.service_track.catalogo.domain.model.MovimentoDeEstoque
import com.clau.service_track.catalogo.domain.model.Reserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import com.clau.service_track.catalogo.domain.vo.TipoDeMovimento
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EstoquePersistenceMapperTest {

    private val mapper = EstoquePersistenceMapper()

    private val insumo = DomainId.de(UUID.randomUUID().toString())
    private val ordem = DomainId.de(UUID.randomUUID().toString())
    private val agora = LocalDateTime.of(2026, 10, 11, 8, 0, 0)

    private fun reserva(
        status: StatusDeReserva = StatusDeReserva.ATIVA,
        expiraEm: LocalDateTime? = agora.plusMinutes(30),
        encerramento: LocalDateTime? = null,
    ) = Reserva.reconstituir(
        id = DomainId.de(UUID.randomUUID().toString()),
        insumoId = insumo,
        ordemServicoId = ordem,
        quantidade = BigDecimal("2.500"),
        status = status,
        expiraEm = expiraEm,
        dataCriacao = agora,
        dataEncerramento = encerramento,
    )

    private fun saldo(reservas: List<Reserva> = emptyList()) = SaldoDeInsumo.reconstituir(
        insumoId = insumo,
        unidadeDeMedida = UnidadeDeMedida.LITRO,
        quantidadeDisponivel = BigDecimal("40.000"),
        quantidadeReservada = BigDecimal("2.500"),
        estoqueMinimo = BigDecimal("5.000"),
        versao = 7,
        dataAtualizacao = agora,
        reservas = reservas,
    )

    @Test
    fun `ida e volta do saldo preserva quantidade, versao e unidade`() {
        val original = saldo()
        val entidade = mapper.paraEntidade(original)
        entidade.versao = original.versao

        val devolta = mapper.paraDominio(entidade, emptyList())

        assertEquals(original.insumoId, devolta.insumoId)
        assertEquals(original.unidadeDeMedida, devolta.unidadeDeMedida)
        assertEquals(original.quantidadeDisponivel, devolta.quantidadeDisponivel)
        assertEquals(original.quantidadeReservada, devolta.quantidadeReservada)
        assertEquals(original.estoqueMinimo, devolta.estoqueMinimo)
        assertEquals(original.versao, devolta.versao)
        assertEquals(original.dataAtualizacao, devolta.dataAtualizacao)
    }

    @Test
    fun `as reservas do saldo voltam pelo segundo argumento, nao pela entidade`() {
        val ativa = reserva()
        val entidade = mapper.paraEntidade(saldo(listOf(ativa)))

        val devolta = mapper.paraDominio(entidade, listOf(mapper.paraEntidade(ativa)))

        assertEquals(1, devolta.reservasAtivas.size)
        assertEquals(ativa.id, devolta.reservasAtivas.single().id)
        assertEquals(ativa, devolta.reservaAtivaDe(ordem))
    }

    @Test
    fun `ida e volta da reserva preserva prazo, status e encerramento`() {
        val original = reserva(
            status = StatusDeReserva.CONSUMIDA,
            encerramento = agora.plusMinutes(10),
        )

        val devolta = mapper.paraDominio(mapper.paraEntidade(original))

        assertEquals(original.id, devolta.id)
        assertEquals(original.insumoId, devolta.insumoId)
        assertEquals(original.ordemServicoId, devolta.ordemServicoId)
        assertEquals(original.quantidade, devolta.quantidade)
        assertEquals(original.status, devolta.status)
        assertEquals(original.expiraEm, devolta.expiraEm)
        assertEquals(original.dataCriacao, devolta.dataCriacao)
        assertEquals(original.dataEncerramento, devolta.dataEncerramento)
    }

    @Test
    fun `reserva sem prazo e sem encerramento nao inventa data`() {
        val devolta = mapper.paraDominio(
            mapper.paraEntidade(reserva(expiraEm = null, encerramento = null))
        )

        assertNull(devolta.expiraEm)
        assertNull(devolta.dataEncerramento)
    }

    @Test
    fun `atualizar saldo mexe nas quantidades e preserva a unidade de medida`() {
        val entidade = mapper.paraEntidade(saldo())
        val unidadeOriginal = entidade.unidadeMedida

        val novo = SaldoDeInsumo.reconstituir(
            insumoId = insumo,
            unidadeDeMedida = UnidadeDeMedida.GALAO,
            quantidadeDisponivel = BigDecimal("12.000"),
            quantidadeReservada = BigDecimal("8.000"),
            estoqueMinimo = BigDecimal("1.000"),
            versao = 9,
            dataAtualizacao = agora.plusHours(3),
            reservas = emptyList(),
        )

        val resultado = mapper.atualizarEntidade(entidade, novo)

        assertTrue(resultado === entidade, "atualizar tem de mexer na instancia gerenciada pelo JPA")
        assertEquals(BigDecimal("12.000"), resultado.quantidadeDisponivel)
        assertEquals(BigDecimal("8.000"), resultado.quantidadeReservada)
        assertEquals(BigDecimal("1.000"), resultado.estoqueMinimo)
        assertEquals(agora.plusHours(3), resultado.dataAtualizacao.toLocalDateTime())
        assertEquals(
            unidadeOriginal,
            resultado.unidadeMedida,
            "unidade de medida do saldo nao muda por atualizacao: mudaria o significado do numero",
        )
    }

    @Test
    fun `atualizar reserva mexe so em status e encerramento`() {
        val entidade = mapper.paraEntidade(reserva())
        val quantidadeOriginal = entidade.quantidade
        val criacaoOriginal = entidade.dataCriacao

        val resultado = mapper.atualizarEntidade(
            entidade,
            reserva(status = StatusDeReserva.LIBERADA, encerramento = agora.plusMinutes(45)),
        )

        assertEquals(StatusDeReserva.LIBERADA, resultado.status)
        assertEquals(agora.plusMinutes(45), resultado.dataEncerramento?.toLocalDateTime())
        assertEquals(quantidadeOriginal, resultado.quantidade)
        assertEquals(criacaoOriginal, resultado.dataCriacao)
    }

    @Test
    fun `movimento vai para a entidade com custo, origem e chave de idempotencia`() {
        val reservaDoMovimento = DomainId.de(UUID.randomUUID().toString())
        val movimento = MovimentoDeEstoque(
            id = DomainId.de(UUID.randomUUID().toString()),
            insumoId = insumo,
            reservaId = reservaDoMovimento,
            tipo = TipoDeMovimento.CONSUMO_RESERVA,
            quantidade = BigDecimal("2.500"),
            unidadeDeMedida = UnidadeDeMedida.LITRO,
            custoUnitario = ValorMonetario.de("31.90"),
            saldoDisponivelApos = BigDecimal("37.500"),
            origemTipo = OrigemDeMovimento.ORDEM_SERVICO,
            origemId = ordem,
            chaveIdempotencia = "ConsumirReserva:chave-1",
            registradoPor = null,
            dataCriacao = agora,
        )

        val entidade = mapper.paraEntidade(movimento)

        assertEquals(UUID.fromString(movimento.id.value), entidade.id)
        assertEquals(UUID.fromString(reservaDoMovimento.value), entidade.reservaId)
        assertEquals(TipoDeMovimento.CONSUMO_RESERVA, entidade.tipo)
        assertEquals(BigDecimal("31.90"), entidade.custoUnitario)
        assertEquals(OrigemDeMovimento.ORDEM_SERVICO, entidade.origemTipo)
        assertEquals(UUID.fromString(ordem.value), entidade.origemId)
        assertEquals("ConsumirReserva:chave-1", entidade.chaveIdempotencia)
        assertNull(entidade.registradoPor)
        assertEquals(agora, entidade.dataCriacao.toLocalDateTime())
    }

    @Test
    fun `evento da outbox carrega chave de particao e datacao propria em UTC`() {
        val idMensagem = UUID.randomUUID()
        val antes = OffsetDateTime.now(ZoneOffset.UTC)

        val entidade = mapper.paraEntidade(
            EventoParaPublicar(
                idMensagem = idMensagem.toString(),
                agregadoTipo = "SaldoDeInsumo",
                agregadoId = insumo,
                chaveDeParticao = insumo.value,
                tipoEvento = "EstoqueReservado",
                versaoEvento = 1,
                payload = """{"tipo":"EstoqueReservado"}""",
                traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            )
        )

        assertEquals(idMensagem, entidade.id)
        assertEquals("SaldoDeInsumo", entidade.agregadoTipo)
        assertEquals(UUID.fromString(insumo.value), entidade.agregadoId)
        assertEquals(insumo.value, entidade.chaveParticao)
        assertEquals("EstoqueReservado", entidade.tipoEvento)
        assertEquals(1.toShort(), entidade.versaoEvento)
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", entidade.traceId)
        assertEquals(0, entidade.dataCriacao.offset.totalSeconds, "a outbox grava em UTC")
        assertTrue(
            !entidade.dataCriacao.isBefore(antes),
            "a data da outbox e do momento da gravacao, nao do evento",
        )
    }
}
