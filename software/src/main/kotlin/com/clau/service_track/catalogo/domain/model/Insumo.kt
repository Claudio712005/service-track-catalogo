package com.clau.service_track.catalogo.domain.model

import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.Especificacao
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime

class Insumo private constructor(
    val id: DomainId,
    val categoriaId: DomainId,
    val nome: String,
    val descricao: String,
    val unidadeDeMedida: UnidadeDeMedida,
    val custo: ValorMonetario,
    val estoqueMinimo: BigDecimal,
    val dataCriacao: LocalDateTime,
    especificacao: Especificacao,
    dataAtualizacao: LocalDateTime,
    qtdEstoque: BigDecimal,
    ativo: Boolean,
) {

    var especificacao: Especificacao = especificacao
        private set

    var dataAtualizacao: LocalDateTime = dataAtualizacao
        private set

    var qtdEstoque: BigDecimal = qtdEstoque
        private set

    var ativo: Boolean = ativo
        private set

    val abaixoDoEstoqueMinimo: Boolean
        get() = qtdEstoque < estoqueMinimo

    fun desativar() {
        if (!ativo) throw ConflitoDeEstadoException("Insumo já está desativado")
        ativo = false
        marcarAtualizacao()
    }

    fun reservar(qtdNecessaria: BigDecimal) {
        exigirQuantidadePositiva(qtdNecessaria)
        exigirCompativelComUnidade(qtdNecessaria)
        if (qtdNecessaria > qtdEstoque) {
            throw ConflitoDeEstadoException(
                "Quantidade necessária (${formatar(qtdNecessaria)}) excede o estoque disponível " +
                    "(${formatar(qtdEstoque)})"
            )
        }
        qtdEstoque -= qtdNecessaria
        marcarAtualizacao()
    }

    fun adicionarAoEstoque(qtdAdicional: BigDecimal) {
        exigirQuantidadePositiva(qtdAdicional)
        exigirCompativelComUnidade(qtdAdicional)
        qtdEstoque += qtdAdicional
        marcarAtualizacao()
    }

    fun calcularCusto(quantidade: BigDecimal): ValorMonetario {
        exigirQuantidadePositiva(quantidade)
        return custo * quantidade
    }

    fun reespecificar(categoria: CategoriaDeInsumo, brutos: Map<String, String>) {
        if (categoria.id != categoriaId) {
            throw DomainException("Categoria informada não é a do insumo '$nome'")
        }
        especificacao = Especificacao.de(categoria, brutos)
        marcarAtualizacao()
    }

    private fun exigirQuantidadePositiva(quantidade: BigDecimal) {
        if (quantidade <= BigDecimal.ZERO) {
            throw DomainException("A quantidade deve ser maior que zero")
        }
    }

    private fun exigirCompativelComUnidade(quantidade: BigDecimal) {
        if (!unidadeDeMedida.fracionavel && quantidade.stripTrailingZeros().scale() > 0) {
            throw DomainException(
                "A unidade ${unidadeDeMedida.simbolo} não admite fração; informe uma quantidade inteira"
            )
        }
    }

    private fun formatar(quantidade: BigDecimal): String =
        "${quantidade.stripTrailingZeros().toPlainString()} ${unidadeDeMedida.simbolo}"

    private fun marcarAtualizacao() {
        dataAtualizacao = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean = other is Insumo && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Insumo(id=$id, nome=$nome)"

    companion object {

        fun criar(
            categoria: CategoriaDeInsumo,
            nome: String,
            descricao: String,
            custo: ValorMonetario,
            especificacao: Map<String, String> = emptyMap(),
            unidadeDeMedida: UnidadeDeMedida = categoria.unidadePadrao,
            qtdEstoqueInicial: BigDecimal = BigDecimal.ZERO,
            estoqueMinimo: BigDecimal = BigDecimal.ZERO,
        ): Insumo {
            if (nome.isBlank()) throw DomainException("Nome do insumo não pode ser vazio")
            if (qtdEstoqueInicial < BigDecimal.ZERO) {
                throw DomainException("Quantidade inicial de estoque não pode ser negativa")
            }
            if (estoqueMinimo < BigDecimal.ZERO) {
                throw DomainException("Estoque mínimo não pode ser negativo")
            }

            val agora = LocalDateTime.now()

            return Insumo(
                id = DomainId.gerar(),
                categoriaId = categoria.id,
                nome = nome,
                descricao = descricao,
                unidadeDeMedida = unidadeDeMedida,
                custo = custo,
                estoqueMinimo = estoqueMinimo,
                dataCriacao = agora,
                especificacao = Especificacao.de(categoria, especificacao),
                dataAtualizacao = agora,
                qtdEstoque = qtdEstoqueInicial,
                ativo = true,
            )
        }

        fun reconstituir(
            id: DomainId,
            categoriaId: DomainId,
            nome: String,
            descricao: String,
            unidadeDeMedida: UnidadeDeMedida,
            custo: ValorMonetario,
            estoqueMinimo: BigDecimal,
            qtdEstoque: BigDecimal,
            especificacao: Especificacao,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
            ativo: Boolean = true,
        ): Insumo = Insumo(
            id = id,
            categoriaId = categoriaId,
            nome = nome,
            descricao = descricao,
            unidadeDeMedida = unidadeDeMedida,
            custo = custo,
            estoqueMinimo = estoqueMinimo,
            dataCriacao = dataCriacao,
            especificacao = especificacao,
            dataAtualizacao = dataAtualizacao,
            qtdEstoque = qtdEstoque,
            ativo = ativo,
        )
    }
}
