package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper

import com.clau.service_track.catalogo.application.port.`in`.api.dto.AtualizarInsumoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarInsumoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.InsumoResponse
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.AtualizarInsumoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.CriarInsumoCommand
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import org.springframework.stereotype.Component

@Component
class InsumoWebMapper(
    private val categorias: CategoriaWebMapper,
) {

    fun paraComando(requisicao: CriarInsumoRequest) = CriarInsumoCommand(
        categoriaId = categorias.paraIdentificador(requisicao.categoriaId),
        sku = requisicao.sku,
        nome = requisicao.nome,
        descricao = requisicao.descricao,
        custo = ValorMonetario.de(requisicao.custo),
        unidadeDeMedida = requisicao.unidadeDeMedida?.let(categorias::paraUnidade),
        especificacao = normalizar(requisicao.especificacao),
        marca = requisicao.marca,
        fabricante = requisicao.fabricante,
        codigoFabricante = requisicao.codigoFabricante,
        codigoBarras = requisicao.codigoBarras,
        controlaLote = requisicao.controlaLote,
        validadeEmDias = requisicao.validadeEmDias,
    )

    fun paraComando(id: String, requisicao: AtualizarInsumoRequest) = AtualizarInsumoCommand(
        id = categorias.paraIdentificador(id),
        descricao = requisicao.descricao,
        custo = ValorMonetario.de(requisicao.custo),
        especificacao = normalizar(requisicao.especificacao),
        marca = requisicao.marca,
        fabricante = requisicao.fabricante,
        codigoFabricante = requisicao.codigoFabricante,
        codigoBarras = requisicao.codigoBarras,
        controlaLote = requisicao.controlaLote,
        validadeEmDias = requisicao.validadeEmDias,
    )

    fun paraResposta(insumo: Insumo) = InsumoResponse(
        id = insumo.id.value,
        categoriaId = insumo.categoriaId.value,
        sku = insumo.sku,
        nome = insumo.nome,
        descricao = insumo.descricao,
        unidadeDeMedida = insumo.unidadeDeMedida.name,
        unidadeFracionavel = insumo.unidadeDeMedida.fracionavel,
        custo = insumo.custo.valor,
        especificacao = insumo.especificacao.paraPersistencia(),
        marca = insumo.marca,
        fabricante = insumo.fabricante,
        codigoFabricante = insumo.codigoFabricante,
        codigoBarras = insumo.codigoBarras,
        controlaLote = insumo.controlaLote,
        validadeEmDias = insumo.validadeEmDias,
        ativo = insumo.ativo,
        dataCriacao = insumo.dataCriacao,
        dataAtualizacao = insumo.dataAtualizacao,
    )

    fun paraResposta(insumos: List<Insumo>) = insumos.map(::paraResposta)

    private fun normalizar(bruto: Map<String, String>): Map<String, String> = bruto
        .mapKeys { it.key.trim() }
        .filterKeys { it.isNotBlank() }
        .mapValues { it.value.trim() }
}
