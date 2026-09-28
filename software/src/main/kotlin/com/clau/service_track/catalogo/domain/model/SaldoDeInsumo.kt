package com.clau.service_track.catalogo.domain.model

import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.StatusDeReserva
import com.clau.service_track.catalogo.domain.vo.TipoDeMovimento
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime

class SaldoDeInsumo private constructor(
    val insumoId: DomainId,
    val unidadeDeMedida: UnidadeDeMedida,
    quantidadeDisponivel: BigDecimal,
    quantidadeReservada: BigDecimal,
    estoqueMinimo: BigDecimal,
    versao: Int,
    dataAtualizacao: LocalDateTime,
    reservas: List<Reserva>,
) {

    var quantidadeDisponivel: BigDecimal = quantidadeDisponivel
        private set

    var quantidadeReservada: BigDecimal = quantidadeReservada
        private set

    var estoqueMinimo: BigDecimal = estoqueMinimo
        private set

    var versao: Int = versao
        private set

    var dataAtualizacao: LocalDateTime = dataAtualizacao
        private set

    private val reservas: MutableList<Reserva> = reservas.toMutableList()

    val reservasAtivas: List<Reserva>
        get() = reservas.filter { it.ativa }

    val abaixoDoMinimo: Boolean
        get() = quantidadeDisponivel < estoqueMinimo

    fun reservaAtivaDe(ordemServicoId: DomainId): Reserva? =
        reservas.firstOrNull { it.ativa && it.ordemServicoId == ordemServicoId }

    fun definirEstoqueMinimo(minimo: BigDecimal) {
        if (minimo < BigDecimal.ZERO) throw DomainException("Estoque mínimo não pode ser negativo")
        estoqueMinimo = minimo
        marcarAtualizacao()
    }

    fun registrarEntrada(
        quantidade: BigDecimal,
        custoUnitario: ValorMonetario?,
        origemTipo: OrigemDeMovimento,
        origemId: DomainId?,
        chaveIdempotencia: String,
        registradoPor: DomainId?,
    ): MovimentoDeEstoque {
        exigirQuantidadeValida(quantidade)
        quantidadeDisponivel += quantidade
        marcarAtualizacao()

        return movimento(
            tipo = TipoDeMovimento.ENTRADA,
            quantidade = quantidade,
            custoUnitario = custoUnitario,
            reservaId = null,
            origemTipo = origemTipo,
            origemId = origemId,
            chaveIdempotencia = chaveIdempotencia,
            registradoPor = registradoPor,
        )
    }

    fun reservar(
        ordemServicoId: DomainId,
        quantidade: BigDecimal,
        expiraEm: LocalDateTime?,
        chaveIdempotencia: String,
    ): ResultadoDeReserva {
        exigirQuantidadeValida(quantidade)

        reservaAtivaDe(ordemServicoId)?.let {
            throw ConflitoDeEstadoException(
                "Ordem de serviço ${ordemServicoId.value} já possui reserva ativa deste insumo"
            )
        }

        if (quantidade > quantidadeDisponivel) {
            throw SaldoInsuficienteException(
                insumoId = insumoId,
                solicitado = quantidade,
                disponivel = quantidadeDisponivel,
                unidade = unidadeDeMedida,
            )
        }

        val reserva = Reserva.criar(insumoId, ordemServicoId, quantidade, expiraEm)
        reservas += reserva

        quantidadeDisponivel -= quantidade
        quantidadeReservada += quantidade
        marcarAtualizacao()

        return ResultadoDeReserva(
            reserva = reserva,
            movimento = movimento(
                tipo = TipoDeMovimento.RESERVA,
                quantidade = quantidade,
                custoUnitario = null,
                reservaId = reserva.id,
                origemTipo = OrigemDeMovimento.ORDEM_SERVICO,
                origemId = ordemServicoId,
                chaveIdempotencia = chaveIdempotencia,
                registradoPor = null,
            ),
        )
    }

    fun consumirReserva(ordemServicoId: DomainId, chaveIdempotencia: String): ResultadoDeReserva {
        val reserva = exigirReservaAtiva(ordemServicoId)
        val agora = LocalDateTime.now()

        reserva.encerrar(StatusDeReserva.CONSUMIDA, agora)
        quantidadeReservada -= reserva.quantidade
        marcarAtualizacao()

        return ResultadoDeReserva(
            reserva = reserva,
            movimento = movimento(
                tipo = TipoDeMovimento.CONSUMO_RESERVA,
                quantidade = reserva.quantidade,
                custoUnitario = null,
                reservaId = reserva.id,
                origemTipo = OrigemDeMovimento.ORDEM_SERVICO,
                origemId = ordemServicoId,
                chaveIdempotencia = chaveIdempotencia,
                registradoPor = null,
            ),
        )
    }

    fun liberarReserva(ordemServicoId: DomainId, chaveIdempotencia: String): ResultadoDeReserva =
        encerrarDevolvendo(exigirReservaAtiva(ordemServicoId), StatusDeReserva.LIBERADA, chaveIdempotencia)

    fun expirarReserva(reserva: Reserva, chaveIdempotencia: String): ResultadoDeReserva {
        if (!reserva.ativa) {
            throw ConflitoDeEstadoException("Reserva ${reserva.id.value} não está ativa")
        }
        return encerrarDevolvendo(reserva, StatusDeReserva.EXPIRADA, chaveIdempotencia)
    }

    private fun encerrarDevolvendo(
        reserva: Reserva,
        status: StatusDeReserva,
        chaveIdempotencia: String,
    ): ResultadoDeReserva {
        val agora = LocalDateTime.now()

        reserva.encerrar(status, agora)
        quantidadeDisponivel += reserva.quantidade
        quantidadeReservada -= reserva.quantidade
        marcarAtualizacao()

        return ResultadoDeReserva(
            reserva = reserva,
            movimento = movimento(
                tipo = TipoDeMovimento.LIBERACAO_RESERVA,
                quantidade = reserva.quantidade,
                custoUnitario = null,
                reservaId = reserva.id,
                origemTipo = OrigemDeMovimento.ORDEM_SERVICO,
                origemId = reserva.ordemServicoId,
                chaveIdempotencia = chaveIdempotencia,
                registradoPor = null,
            ),
        )
    }

    private fun exigirReservaAtiva(ordemServicoId: DomainId): Reserva = reservaAtivaDe(ordemServicoId)
        ?: throw ConflitoDeEstadoException(
            "Nenhuma reserva ativa deste insumo para a ordem de serviço ${ordemServicoId.value}"
        )

    private fun movimento(
        tipo: TipoDeMovimento,
        quantidade: BigDecimal,
        custoUnitario: ValorMonetario?,
        reservaId: DomainId?,
        origemTipo: OrigemDeMovimento,
        origemId: DomainId?,
        chaveIdempotencia: String,
        registradoPor: DomainId?,
    ) = MovimentoDeEstoque(
        id = DomainId.gerar(),
        insumoId = insumoId,
        reservaId = reservaId,
        tipo = tipo,
        quantidade = quantidade,
        unidadeDeMedida = unidadeDeMedida,
        custoUnitario = custoUnitario,
        saldoDisponivelApos = quantidadeDisponivel,
        origemTipo = origemTipo,
        origemId = origemId,
        chaveIdempotencia = chaveIdempotencia,
        registradoPor = registradoPor,
        dataCriacao = dataAtualizacao,
    )

    private fun exigirQuantidadeValida(quantidade: BigDecimal) {
        if (quantidade <= BigDecimal.ZERO) {
            throw DomainException("A quantidade deve ser maior que zero")
        }
        if (!unidadeDeMedida.fracionavel && quantidade.stripTrailingZeros().scale() > 0) {
            throw DomainException(
                "A unidade ${unidadeDeMedida.simbolo} não admite fração; informe uma quantidade inteira"
            )
        }
    }

    private fun marcarAtualizacao() {
        dataAtualizacao = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean = other is SaldoDeInsumo && other.insumoId == insumoId

    override fun hashCode(): Int = insumoId.hashCode()

    override fun toString(): String =
        "SaldoDeInsumo(insumoId=$insumoId, disponivel=$quantidadeDisponivel, reservada=$quantidadeReservada)"

    companion object {

        fun zerado(insumoId: DomainId, unidadeDeMedida: UnidadeDeMedida): SaldoDeInsumo = SaldoDeInsumo(
            insumoId = insumoId,
            unidadeDeMedida = unidadeDeMedida,
            quantidadeDisponivel = BigDecimal.ZERO,
            quantidadeReservada = BigDecimal.ZERO,
            estoqueMinimo = BigDecimal.ZERO,
            versao = 0,
            dataAtualizacao = LocalDateTime.now(),
            reservas = emptyList(),
        )

        fun reconstituir(
            insumoId: DomainId,
            unidadeDeMedida: UnidadeDeMedida,
            quantidadeDisponivel: BigDecimal,
            quantidadeReservada: BigDecimal,
            estoqueMinimo: BigDecimal,
            versao: Int,
            dataAtualizacao: LocalDateTime,
            reservas: List<Reserva>,
        ): SaldoDeInsumo = SaldoDeInsumo(
            insumoId = insumoId,
            unidadeDeMedida = unidadeDeMedida,
            quantidadeDisponivel = quantidadeDisponivel,
            quantidadeReservada = quantidadeReservada,
            estoqueMinimo = estoqueMinimo,
            versao = versao,
            dataAtualizacao = dataAtualizacao,
            reservas = reservas,
        )
    }
}
