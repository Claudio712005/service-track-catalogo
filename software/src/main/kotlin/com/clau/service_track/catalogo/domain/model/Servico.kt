package com.clau.service_track.catalogo.domain.model

import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.time.LocalDateTime

class Servico private constructor(
    val id: DomainId,
    val nome: String,
    val dataCriacao: LocalDateTime,
    descricao: String,
    valorReferencia: ValorMonetario?,
    dataAtualizacao: LocalDateTime,
    ativo: Boolean,
) {

    var descricao: String = descricao
        private set

    var valorReferencia: ValorMonetario? = valorReferencia
        private set

    var dataAtualizacao: LocalDateTime = dataAtualizacao
        private set

    var ativo: Boolean = ativo
        private set

    fun desativar() {
        if (!ativo) {
            throw ConflitoDeEstadoException("Serviço já está desativado")
        }
        ativo = false
        marcarAtualizacao()
    }

    fun atualizarValorReferencia(novoValor: ValorMonetario) {
        valorReferencia = novoValor
        marcarAtualizacao()
    }

    fun atualizarDescricao(novaDescricao: String) {
        if (novaDescricao.isBlank()) {
            throw DomainException("Descrição do serviço não pode ser vazia")
        }
        descricao = novaDescricao
        marcarAtualizacao()
    }

    private fun marcarAtualizacao() {
        dataAtualizacao = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean = other is Servico && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Servico(id=$id, nome=$nome)"

    companion object {

        fun criar(
            nome: String,
            descricao: String,
            valorReferencia: ValorMonetario? = null,
        ): Servico {
            if (nome.isBlank()) {
                throw DomainException("Nome do serviço não pode ser vazio")
            }
            if (descricao.isBlank()) {
                throw DomainException("Descrição do serviço não pode ser vazia")
            }

            val agora = LocalDateTime.now()

            return Servico(
                id = DomainId.gerar(),
                nome = nome,
                descricao = descricao,
                valorReferencia = valorReferencia,
                dataCriacao = agora,
                dataAtualizacao = agora,
                ativo = true,
            )
        }

        fun reconstituir(
            id: DomainId,
            nome: String,
            descricao: String,
            valorReferencia: ValorMonetario?,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
            ativo: Boolean = true,
        ): Servico = Servico(
            id = id,
            nome = nome,
            descricao = descricao,
            valorReferencia = valorReferencia,
            dataCriacao = dataCriacao,
            dataAtualizacao = dataAtualizacao,
            ativo = ativo,
        )
    }
}
