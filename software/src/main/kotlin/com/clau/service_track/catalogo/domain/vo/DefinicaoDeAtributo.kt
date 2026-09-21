package com.clau.service_track.catalogo.domain.vo

import com.clau.service_track.catalogo.domain.exception.DomainException
import java.math.BigDecimal

data class DefinicaoDeAtributo(
    val chave: String,
    val rotulo: String,
    val tipo: TipoDeAtributo,
    val unidade: String? = null,
    val obrigatorio: Boolean = false,
    val opcoes: List<String> = emptyList(),
) {

    init {
        if (chave.isBlank()) throw DomainException("Chave do atributo não pode ser vazia")
        if (!chave.matches(FORMATO_DA_CHAVE)) {
            throw DomainException(
                "Chave '$chave' inválida. Use letras minúsculas, dígitos e hífen, começando por letra"
            )
        }
        if (tipo == TipoDeAtributo.OPCAO && opcoes.isEmpty()) {
            throw DomainException("Atributo '$chave' é do tipo OPCAO e precisa declarar as opções aceitas")
        }
        if (tipo != TipoDeAtributo.OPCAO && opcoes.isNotEmpty()) {
            throw DomainException("Atributo '$chave' declara opções, mas não é do tipo OPCAO")
        }
    }

    fun converter(bruto: String): ValorDeAtributo = when (tipo) {
        TipoDeAtributo.TEXTO -> ValorDeAtributo.Texto(exigirNaoVazio(bruto))

        TipoDeAtributo.INTEIRO -> ValorDeAtributo.Inteiro(
            bruto.trim().toIntOrNull() ?: recusar(bruto, "número inteiro")
        )

        TipoDeAtributo.DECIMAL -> ValorDeAtributo.Decimal(
            runCatching { BigDecimal(bruto.trim()) }.getOrElse { recusar(bruto, "número decimal") }
        )

        TipoDeAtributo.BOOLEANO -> ValorDeAtributo.Booleano(
            when (bruto.trim().lowercase()) {
                "true", "sim", "1" -> true
                "false", "nao", "não", "0" -> false
                else -> recusar(bruto, "booleano")
            }
        )

        TipoDeAtributo.OPCAO -> ValorDeAtributo.Opcao(
            opcoes.firstOrNull { it.equals(bruto.trim(), ignoreCase = true) }
                ?: throw DomainException(
                    "Valor '$bruto' não é aceito em '$rotulo'. Opções: ${opcoes.joinToString(", ")}"
                )
        )
    }

    private fun exigirNaoVazio(bruto: String): String =
        bruto.trim().ifBlank { throw DomainException("Atributo '$rotulo' não pode ser vazio") }

    private fun recusar(bruto: String, esperado: String): Nothing =
        throw DomainException("Valor '$bruto' em '$rotulo' não é um $esperado válido")

    companion object {
        private val FORMATO_DA_CHAVE = Regex("^[a-z][a-z0-9-]*$")
    }
}
