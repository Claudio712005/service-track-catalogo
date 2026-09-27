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
    val sku: String,
    val nome: String,
    val unidadeDeMedida: UnidadeDeMedida,
    val estoqueMinimo: BigDecimal,
    val dataCriacao: LocalDateTime,
    descricao: String,
    custo: ValorMonetario,
    marca: String?,
    fabricante: String?,
    codigoFabricante: String?,
    codigoBarras: String?,
    controlaLote: Boolean,
    validadeEmDias: Int?,
    especificacao: Especificacao,
    dataAtualizacao: LocalDateTime,
    qtdEstoque: BigDecimal,
    ativo: Boolean,
) {

    var descricao: String = descricao
        private set

    var custo: ValorMonetario = custo
        private set

    var marca: String? = marca
        private set

    var fabricante: String? = fabricante
        private set

    var codigoFabricante: String? = codigoFabricante
        private set

    var codigoBarras: String? = codigoBarras
        private set

    var controlaLote: Boolean = controlaLote
        private set

    var validadeEmDias: Int? = validadeEmDias
        private set

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

    fun atualizarDescricao(novaDescricao: String) {
        exigirAtivo()
        descricao = novaDescricao.trim()
        marcarAtualizacao()
    }

    fun atualizarCusto(novoCusto: ValorMonetario) {
        exigirAtivo()
        custo = novoCusto
        marcarAtualizacao()
    }

    fun atualizarIdentificacaoComercial(
        marca: String?,
        fabricante: String?,
        codigoFabricante: String?,
        codigoBarras: String?,
    ) {
        exigirAtivo()
        this.marca = normalizar(marca)
        this.fabricante = normalizar(fabricante)
        this.codigoFabricante = normalizar(codigoFabricante)
        this.codigoBarras = validarCodigoBarras(normalizar(codigoBarras))
        marcarAtualizacao()
    }

    fun definirControleDeLote(controla: Boolean, validadeEmDias: Int?) {
        exigirAtivo()
        if (validadeEmDias != null && validadeEmDias < 0) {
            throw DomainException("Prazo de validade não pode ser negativo")
        }
        this.controlaLote = controla
        this.validadeEmDias = validadeEmDias
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
        exigirAtivo()
        exigirCategoriaCorrespondente(categoria)
        especificacao = Especificacao.de(categoria, brutos)
        marcarAtualizacao()
    }

    fun descreverEspecificacao(categoria: CategoriaDeInsumo): String {
        exigirCategoriaCorrespondente(categoria)
        return especificacao.descrever(categoria)
    }

    private fun exigirCategoriaCorrespondente(categoria: CategoriaDeInsumo) {
        if (categoria.id != categoriaId) {
            throw DomainException("Categoria informada não é a do insumo '$nome'")
        }
    }

    private fun exigirAtivo() {
        if (!ativo) {
            throw ConflitoDeEstadoException("Insumo '$nome' está desativado e não pode ser alterado")
        }
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

    override fun toString(): String = "Insumo(id=$id, sku=$sku)"

    companion object {

        private val FORMATO_DO_SKU = Regex("^[A-Z0-9][A-Z0-9._-]{2,39}$")
        private val FORMATO_DO_CODIGO_DE_BARRAS = Regex("^\\d{8,14}$")

        fun criar(
            categoria: CategoriaDeInsumo,
            sku: String,
            nome: String,
            descricao: String,
            custo: ValorMonetario,
            especificacao: Map<String, String> = emptyMap(),
            unidadeDeMedida: UnidadeDeMedida = categoria.unidadePadrao,
            marca: String? = null,
            fabricante: String? = null,
            codigoFabricante: String? = null,
            codigoBarras: String? = null,
            controlaLote: Boolean = false,
            validadeEmDias: Int? = null,
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
            if (validadeEmDias != null && validadeEmDias < 0) {
                throw DomainException("Prazo de validade não pode ser negativo")
            }

            val agora = LocalDateTime.now()

            return Insumo(
                id = DomainId.gerar(),
                categoriaId = categoria.id,
                sku = validarSku(sku),
                nome = nome.trim(),
                unidadeDeMedida = unidadeDeMedida,
                estoqueMinimo = estoqueMinimo,
                dataCriacao = agora,
                descricao = descricao.trim(),
                custo = custo,
                marca = normalizar(marca),
                fabricante = normalizar(fabricante),
                codigoFabricante = normalizar(codigoFabricante),
                codigoBarras = validarCodigoBarras(normalizar(codigoBarras)),
                controlaLote = controlaLote,
                validadeEmDias = validadeEmDias,
                especificacao = Especificacao.de(categoria, especificacao),
                dataAtualizacao = agora,
                qtdEstoque = qtdEstoqueInicial,
                ativo = true,
            )
        }

        fun reconstituir(
            id: DomainId,
            categoriaId: DomainId,
            sku: String,
            nome: String,
            descricao: String,
            unidadeDeMedida: UnidadeDeMedida,
            custo: ValorMonetario,
            especificacao: Especificacao,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
            marca: String? = null,
            fabricante: String? = null,
            codigoFabricante: String? = null,
            codigoBarras: String? = null,
            controlaLote: Boolean = false,
            validadeEmDias: Int? = null,
            estoqueMinimo: BigDecimal = BigDecimal.ZERO,
            qtdEstoque: BigDecimal = BigDecimal.ZERO,
            ativo: Boolean = true,
        ): Insumo = Insumo(
            id = id,
            categoriaId = categoriaId,
            sku = sku,
            nome = nome,
            unidadeDeMedida = unidadeDeMedida,
            estoqueMinimo = estoqueMinimo,
            dataCriacao = dataCriacao,
            descricao = descricao,
            custo = custo,
            marca = marca,
            fabricante = fabricante,
            codigoFabricante = codigoFabricante,
            codigoBarras = codigoBarras,
            controlaLote = controlaLote,
            validadeEmDias = validadeEmDias,
            especificacao = especificacao,
            dataAtualizacao = dataAtualizacao,
            qtdEstoque = qtdEstoque,
            ativo = ativo,
        )

        private fun validarSku(bruto: String): String {
            val normalizado = bruto.trim().uppercase()
            if (!normalizado.matches(FORMATO_DO_SKU)) {
                throw DomainException(
                    "SKU '$bruto' inválido. Use de 3 a 40 caracteres entre letras, dígitos, ponto, " +
                        "hífen e sublinhado, começando por letra ou dígito"
                )
            }
            return normalizado
        }

        private fun validarCodigoBarras(bruto: String?): String? {
            if (bruto == null) return null
            if (!bruto.matches(FORMATO_DO_CODIGO_DE_BARRAS)) {
                throw DomainException("Código de barras '$bruto' inválido. Informe de 8 a 14 dígitos")
            }
            return bruto
        }

        private fun normalizar(bruto: String?): String? = bruto?.trim()?.ifBlank { null }
    }
}
