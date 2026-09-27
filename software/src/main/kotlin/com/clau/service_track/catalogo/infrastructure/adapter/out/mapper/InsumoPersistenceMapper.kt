package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.Especificacao
import com.clau.service_track.catalogo.domain.vo.ValorDeAtributo
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import com.clau.service_track.catalogo.infrastructure.entity.mongo.InsumoDocumento
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class InsumoPersistenceMapper {

    fun paraDocumento(insumo: Insumo): InsumoDocumento = InsumoDocumento(
        id = insumo.id.value,
        categoriaId = insumo.categoriaId.value,
        sku = insumo.sku,
        nome = insumo.nome,
        descricao = insumo.descricao,
        marca = insumo.marca,
        fabricante = insumo.fabricante,
        codigoFabricante = insumo.codigoFabricante,
        codigoBarras = insumo.codigoBarras,
        unidadeMedida = insumo.unidadeDeMedida,
        custoPadrao = insumo.custo.valor,
        controlaLote = insumo.controlaLote,
        validadeEmDias = insumo.validadeEmDias,
        especificacao = insumo.especificacao.paraPersistencia().toMutableMap(),
        ativo = insumo.ativo,
        dataCriacao = paraInstante(insumo.dataCriacao),
        dataAtualizacao = paraInstante(insumo.dataAtualizacao),
    )

    fun atualizarDocumento(documento: InsumoDocumento, insumo: Insumo): InsumoDocumento = documento.apply {
        descricao = insumo.descricao
        marca = insumo.marca
        fabricante = insumo.fabricante
        codigoFabricante = insumo.codigoFabricante
        codigoBarras = insumo.codigoBarras
        custoPadrao = insumo.custo.valor
        controlaLote = insumo.controlaLote
        validadeEmDias = insumo.validadeEmDias
        especificacao = insumo.especificacao.paraPersistencia().toMutableMap()
        ativo = insumo.ativo
        dataAtualizacao = paraInstante(insumo.dataAtualizacao)
    }

    fun paraDominio(documento: InsumoDocumento, categoria: CategoriaDeInsumo?): Insumo = Insumo.reconstituir(
        id = DomainId.de(documento.id),
        categoriaId = DomainId.de(documento.categoriaId),
        sku = documento.sku,
        nome = documento.nome,
        descricao = documento.descricao,
        unidadeDeMedida = documento.unidadeMedida,
        custo = ValorMonetario.de(documento.custoPadrao),
        especificacao = reconstituirEspecificacao(documento.especificacao, categoria),
        dataCriacao = paraLocal(documento.dataCriacao),
        dataAtualizacao = paraLocal(documento.dataAtualizacao),
        marca = documento.marca,
        fabricante = documento.fabricante,
        codigoFabricante = documento.codigoFabricante,
        codigoBarras = documento.codigoBarras,
        controlaLote = documento.controlaLote,
        validadeEmDias = documento.validadeEmDias,
        ativo = documento.ativo,
    )

    private fun reconstituirEspecificacao(
        brutos: Map<String, String>,
        categoria: CategoriaDeInsumo?,
    ): Especificacao {
        if (brutos.isEmpty()) return Especificacao.VAZIA

        val valores = brutos.mapValues { (chave, bruto) ->
            val definicao = categoria?.definicaoDe(chave)
            definicao?.let { runCatching { it.converter(bruto) }.getOrNull() } ?: ValorDeAtributo.Texto(bruto)
        }
        return Especificacao.reconstituir(valores)
    }

    private fun paraInstante(momento: LocalDateTime): Instant = momento.toInstant(ZoneOffset.UTC)

    private fun paraLocal(momento: Instant): LocalDateTime = LocalDateTime.ofInstant(momento, ZoneOffset.UTC)
}
