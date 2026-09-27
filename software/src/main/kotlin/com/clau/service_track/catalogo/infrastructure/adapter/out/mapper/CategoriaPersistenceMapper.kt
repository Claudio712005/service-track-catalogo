package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import com.clau.service_track.catalogo.infrastructure.entity.mongo.CategoriaDocumento
import com.clau.service_track.catalogo.infrastructure.entity.mongo.DefinicaoDeAtributoDocumento
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CategoriaPersistenceMapper {

    fun paraDocumento(categoria: CategoriaDeInsumo, criadoEm: Instant?): CategoriaDocumento {
        val agora = Instant.now()
        return CategoriaDocumento(
            id = categoria.id.value,
            codigo = categoria.codigo,
            nome = categoria.nome,
            unidadePadrao = categoria.unidadePadrao,
            ativa = categoria.ativa,
            atributos = categoria.atributos.map {
                DefinicaoDeAtributoDocumento(
                    chave = it.chave,
                    rotulo = it.rotulo,
                    tipo = it.tipo,
                    unidade = it.unidade,
                    obrigatorio = it.obrigatorio,
                    opcoes = it.opcoes.toMutableList(),
                    ordem = 0,
                )
            }.toMutableList(),
            dataCriacao = criadoEm ?: agora,
            dataAtualizacao = agora,
        )
    }

    fun paraDominio(documento: CategoriaDocumento): CategoriaDeInsumo = CategoriaDeInsumo.reconstituir(
        id = DomainId.de(documento.id),
        codigo = documento.codigo,
        nome = documento.nome,
        unidadePadrao = documento.unidadePadrao,
        ativa = documento.ativa,
        atributos = documento.atributos.map {
            DefinicaoDeAtributo(
                chave = it.chave,
                rotulo = it.rotulo,
                tipo = it.tipo,
                unidade = it.unidade,
                obrigatorio = it.obrigatorio,
                opcoes = it.opcoes.toList(),
            )
        },
    )

    fun tipoDe(bruto: String): TipoDeAtributo = TipoDeAtributo.valueOf(bruto.uppercase())
}
