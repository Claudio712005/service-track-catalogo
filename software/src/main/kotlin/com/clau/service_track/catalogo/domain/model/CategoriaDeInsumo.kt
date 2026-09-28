package com.clau.service_track.catalogo.domain.model

import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida

class CategoriaDeInsumo private constructor(
    val id: DomainId,
    val codigo: String,
    val nome: String,
    val unidadePadrao: UnidadeDeMedida,
    atributos: List<DefinicaoDeAtributo>,
    ativa: Boolean,
) {

    var atributos: List<DefinicaoDeAtributo> = atributos
        private set

    var ativa: Boolean = ativa
        private set

    fun definicaoDe(chave: String): DefinicaoDeAtributo? = atributos.firstOrNull { it.chave == chave }

    fun obrigatorios(): List<DefinicaoDeAtributo> = atributos.filter { it.obrigatorio }

    fun desativar() {
        if (!ativa) throw ConflitoDeEstadoException("Categoria '$nome' já está desativada")
        ativa = false
    }

    fun reativar() {
        if (ativa) throw ConflitoDeEstadoException("Categoria '$nome' já está ativa")
        ativa = true
    }

    fun exigirAtivaParaCadastro() {
        if (!ativa) {
            throw ConflitoDeEstadoException(
                "Categoria '$nome' está desativada e não aceita insumo novo. Reative a categoria antes"
            )
        }
    }

    fun acrescentarAtributo(definicao: DefinicaoDeAtributo) {
        if (!ativa) {
            throw ConflitoDeEstadoException("Categoria '$nome' está desativada e não pode receber atributo")
        }
        if (definicaoDe(definicao.chave) != null) {
            throw DomainException("Categoria '$nome' já possui o atributo '${definicao.chave}'")
        }
        if (definicao.obrigatorio) {
            throw DomainException(
                "Atributo '${definicao.chave}' não pode nascer obrigatório: os insumos já cadastrados " +
                    "não o possuem. Cadastre como opcional, preencha o acervo e então torne obrigatório"
            )
        }
        atributos = atributos + definicao
    }

    override fun equals(other: Any?): Boolean = other is CategoriaDeInsumo && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "CategoriaDeInsumo(codigo=$codigo)"

    companion object {

        fun criar(
            codigo: String,
            nome: String,
            unidadePadrao: UnidadeDeMedida,
            atributos: List<DefinicaoDeAtributo> = emptyList(),
        ): CategoriaDeInsumo {
            if (codigo.isBlank()) throw DomainException("Código da categoria não pode ser vazio")
            if (nome.isBlank()) throw DomainException("Nome da categoria não pode ser vazio")

            val repetidas = atributos.groupBy { it.chave }.filterValues { it.size > 1 }.keys
            if (repetidas.isNotEmpty()) {
                throw DomainException("Atributos repetidos na categoria '$nome': ${repetidas.joinToString(", ")}")
            }

            return CategoriaDeInsumo(
                ativa = true,
                id = DomainId.gerar(),
                codigo = codigo.trim().uppercase(),
                nome = nome,
                unidadePadrao = unidadePadrao,
                atributos = atributos,
            )
        }

        fun reconstituir(
            id: DomainId,
            codigo: String,
            nome: String,
            unidadePadrao: UnidadeDeMedida,
            atributos: List<DefinicaoDeAtributo>,
            ativa: Boolean = true,
        ): CategoriaDeInsumo = CategoriaDeInsumo(id, codigo, nome, unidadePadrao, atributos, ativa)
    }
}
