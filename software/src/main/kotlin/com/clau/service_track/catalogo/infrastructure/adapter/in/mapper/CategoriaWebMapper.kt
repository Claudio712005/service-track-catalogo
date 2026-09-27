package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper

import com.clau.service_track.catalogo.application.port.`in`.api.dto.CategoriaResponse
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarCategoriaRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.DefinicaoDeAtributoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.DefinicaoDeAtributoResponse
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AcrescentarAtributoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.CriarCategoriaCommand
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoriaWebMapper {

    fun paraIdentificador(bruto: String): DomainId {
        val normalizado = runCatching { UUID.fromString(bruto.trim()) }
            .getOrElse { throw DomainException("Identificador '$bruto' não é um UUID válido") }
        return DomainId.de(normalizado.toString())
    }

    fun paraUnidade(bruto: String): UnidadeDeMedida = runCatching {
        UnidadeDeMedida.valueOf(bruto.trim().uppercase())
    }.getOrElse {
        throw DomainException(
            "Unidade de medida '$bruto' não existe. Aceitas: ${UnidadeDeMedida.entries.joinToString(", ") { it.name }}"
        )
    }

    fun paraTipo(bruto: String): TipoDeAtributo = runCatching {
        TipoDeAtributo.valueOf(bruto.trim().uppercase())
    }.getOrElse {
        throw DomainException(
            "Tipo de atributo '$bruto' não existe. Aceitos: ${TipoDeAtributo.entries.joinToString(", ") { it.name }}"
        )
    }

    fun paraDefinicao(requisicao: DefinicaoDeAtributoRequest) = DefinicaoDeAtributo(
        chave = requisicao.chave.trim(),
        rotulo = requisicao.rotulo.trim(),
        tipo = paraTipo(requisicao.tipo),
        unidade = requisicao.unidade?.trim()?.ifBlank { null },
        obrigatorio = requisicao.obrigatorio,
        opcoes = requisicao.opcoes.map { it.trim() }.filter { it.isNotBlank() },
    )

    fun paraComando(requisicao: CriarCategoriaRequest) = CriarCategoriaCommand(
        codigo = requisicao.codigo,
        nome = requisicao.nome,
        unidadePadrao = paraUnidade(requisicao.unidadePadrao),
        atributos = requisicao.atributos.map(::paraDefinicao),
    )

    fun paraComando(id: String, requisicao: DefinicaoDeAtributoRequest) = AcrescentarAtributoCommand(
        categoriaId = paraIdentificador(id),
        definicao = paraDefinicao(requisicao),
    )

    fun paraResposta(categoria: CategoriaDeInsumo) = CategoriaResponse(
        id = categoria.id.value,
        codigo = categoria.codigo,
        nome = categoria.nome,
        unidadePadrao = categoria.unidadePadrao.name,
        unidadeFracionavel = categoria.unidadePadrao.fracionavel,
        atributos = categoria.atributos.map {
            DefinicaoDeAtributoResponse(
                chave = it.chave,
                rotulo = it.rotulo,
                tipo = it.tipo.name,
                unidade = it.unidade,
                obrigatorio = it.obrigatorio,
                opcoes = it.opcoes,
            )
        },
    )

    fun paraResposta(categorias: List<CategoriaDeInsumo>) = categorias.map(::paraResposta)
}
