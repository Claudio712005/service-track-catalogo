package com.clau.service_track.catalogo.domain.vo

import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo

class Especificacao private constructor(
    private val valores: Map<String, ValorDeAtributo>,
) {

    operator fun get(chave: String): ValorDeAtributo? = valores[chave]

    fun contem(chave: String): Boolean = valores.containsKey(chave)

    fun comoMapa(): Map<String, ValorDeAtributo> = valores.toMap()

    fun paraPersistencia(): Map<String, String> = valores.mapValues { it.value.comoTexto() }

    fun descrever(categoria: CategoriaDeInsumo): String = categoria.atributos
        .filter { valores.containsKey(it.chave) }
        .joinToString(", ") { definicao ->
            val texto = valores.getValue(definicao.chave).comoTexto()
            val unidade = definicao.unidade?.let { " $it" } ?: ""
            "${definicao.rotulo}: $texto$unidade"
        }

    override fun equals(other: Any?): Boolean = other is Especificacao && other.valores == valores

    override fun hashCode(): Int = valores.hashCode()

    override fun toString(): String = valores.entries.joinToString(", ") { "${it.key}=${it.value.comoTexto()}" }

    companion object {

        val VAZIA = Especificacao(emptyMap())

        fun de(categoria: CategoriaDeInsumo, brutos: Map<String, String>): Especificacao {
            val desconhecidos = brutos.keys - categoria.atributos.map { it.chave }.toSet()
            if (desconhecidos.isNotEmpty()) {
                throw DomainException(
                    "Atributo(s) não previsto(s) na categoria '${categoria.nome}': " +
                        "${desconhecidos.sorted().joinToString(", ")}. " +
                        "Declare na categoria antes de usar"
                )
            }

            val faltantes = categoria.obrigatorios()
                .map { it.chave }
                .filter { brutos[it].isNullOrBlank() }
            if (faltantes.isNotEmpty()) {
                throw DomainException(
                    "Atributo(s) obrigatório(s) ausente(s) em '${categoria.nome}': ${faltantes.joinToString(", ")}"
                )
            }

            val convertidos = brutos
                .filterValues { it.isNotBlank() }
                .mapValues { (chave, bruto) -> categoria.definicaoDe(chave)!!.converter(bruto) }

            return Especificacao(convertidos)
        }

        fun reconstituir(valores: Map<String, ValorDeAtributo>): Especificacao = Especificacao(valores.toMap())
    }
}
