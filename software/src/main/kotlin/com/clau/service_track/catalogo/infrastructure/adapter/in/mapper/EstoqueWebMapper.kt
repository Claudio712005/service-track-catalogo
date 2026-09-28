package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper

import com.clau.service_track.catalogo.application.port.`in`.api.dto.EntradaDeEstoqueRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.ReservaDeEstoqueResponse
import com.clau.service_track.catalogo.application.port.`in`.api.dto.SaldoDeInsumoResponse
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.RegistrarEntradaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ResultadoDeSaldo
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.OrigemDeMovimento
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EstoqueWebMapper {

    fun paraIdentificador(bruto: String): DomainId {
        val normalizado = runCatching { UUID.fromString(bruto.trim()) }
            .getOrElse { throw DomainException("Identificador '$bruto' não é um UUID válido") }
        return DomainId.de(normalizado.toString())
    }

    fun paraComando(
        insumoId: String,
        chaveIdempotencia: String,
        requisicao: EntradaDeEstoqueRequest,
    ): RegistrarEntradaCommand = RegistrarEntradaCommand(
        insumoId = paraIdentificador(insumoId),
        quantidade = requisicao.quantidade,
        custoUnitario = requisicao.custoUnitario?.let(ValorMonetario::de),
        origemTipo = paraOrigem(requisicao.origemTipo),
        origemId = requisicao.origemId?.let(::paraIdentificador),
        chaveIdempotencia = exigirChave(chaveIdempotencia),
        registradoPor = null,
    )

    fun paraResposta(resultado: ResultadoDeSaldo): SaldoDeInsumoResponse =
        paraResposta(resultado.insumo, resultado.saldo)

    fun paraResposta(insumo: Insumo, saldo: SaldoDeInsumo): SaldoDeInsumoResponse = SaldoDeInsumoResponse(
        insumoId = insumo.id.value,
        sku = insumo.sku,
        nome = insumo.nome,
        unidadeDeMedida = saldo.unidadeDeMedida.name,
        quantidadeDisponivel = saldo.quantidadeDisponivel,
        quantidadeReservada = saldo.quantidadeReservada,
        estoqueMinimo = saldo.estoqueMinimo,
        abaixoDoMinimo = saldo.abaixoDoMinimo,
        dataAtualizacao = saldo.dataAtualizacao.toString(),
        reservas = saldo.reservasAtivas.map {
            ReservaDeEstoqueResponse(
                reservaId = it.id.value,
                ordemServicoId = it.ordemServicoId.value,
                quantidade = it.quantidade,
                expiraEm = it.expiraEm?.toString(),
            )
        },
    )

    private fun paraOrigem(bruto: String): OrigemDeMovimento = runCatching {
        OrigemDeMovimento.valueOf(bruto.trim().uppercase())
    }.getOrElse {
        throw DomainException(
            "Origem '$bruto' não existe. Aceitas na entrada: NOTA_ENTRADA, INVENTARIO, AJUSTE_MANUAL"
        )
    }.also {
        if (it == OrigemDeMovimento.ORDEM_SERVICO) {
            throw DomainException(
                "Origem ORDEM_SERVICO não se registra por HTTP: movimento de ordem de serviço " +
                    "nasce de comando na fila"
            )
        }
    }

    private fun exigirChave(bruto: String): String {
        val chave = bruto.trim()
        if (chave.isBlank()) throw DomainException("Cabeçalho X-Idempotency-Key não pode ser vazio")
        if (chave.length > TAMANHO_MAXIMO_DA_CHAVE) {
            throw DomainException("Cabeçalho X-Idempotency-Key excede $TAMANHO_MAXIMO_DA_CHAVE caracteres")
        }
        return chave
    }

    private companion object {
        const val TAMANHO_MAXIMO_DA_CHAVE = 120
    }
}
