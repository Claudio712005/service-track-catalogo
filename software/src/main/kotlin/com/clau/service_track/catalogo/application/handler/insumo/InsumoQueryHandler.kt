package com.clau.service_track.catalogo.application.handler.insumo

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.BuscarInsumoQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.BuscarInsumoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.ListarInsumosQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.ListarInsumosUseCase
import com.clau.service_track.catalogo.application.port.out.repository.FiltroDeInsumo
import com.clau.service_track.catalogo.application.port.out.repository.InsumoRepositoryPort
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.shared.OrdemDeExibicao
import com.clau.service_track.catalogo.shared.annotation.UseCase

@UseCase
class InsumoQueryHandler(
    private val insumos: InsumoRepositoryPort,
) : BuscarInsumoUseCase, ListarInsumosUseCase {

    override fun executar(consulta: BuscarInsumoQuery): Insumo = when {
        consulta.id != null -> insumos.buscarPorId(consulta.id)
            ?: throw RecursoNaoEncontradoException("Insumo", consulta.id.value)

        consulta.sku != null -> insumos.buscarPorSku(consulta.sku.trim().uppercase())
            ?: throw RecursoNaoEncontradoException("Insumo de SKU", consulta.sku)

        else -> throw DomainException("Informe o identificador ou o SKU do insumo")
    }

    override fun executar(consulta: ListarInsumosQuery): List<Insumo> = insumos
        .listar(
            FiltroDeInsumo(
                incluirInativos = consulta.incluirInativos,
                categoriaId = consulta.categoriaId,
                termo = consulta.termo?.trim()?.ifBlank { null },
            )
        )
        .sortedWith(compareBy(OrdemDeExibicao.porNome()) { it.nome })
}
