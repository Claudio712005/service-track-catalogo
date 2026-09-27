package com.clau.service_track.catalogo.application.handler.categoria

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.BuscarCategoriaQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.BuscarCategoriaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.ListarCategoriasQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.ListarCategoriasUseCase
import com.clau.service_track.catalogo.application.port.out.repository.CategoriaRepositoryPort
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.shared.OrdemDeExibicao
import com.clau.service_track.catalogo.shared.annotation.UseCase

@UseCase
class CategoriaQueryHandler(
    private val repositorio: CategoriaRepositoryPort,
) : BuscarCategoriaUseCase, ListarCategoriasUseCase {

    override fun executar(consulta: BuscarCategoriaQuery): CategoriaDeInsumo =
        repositorio.buscarPorId(consulta.id)
            ?: throw RecursoNaoEncontradoException("Categoria", consulta.id.value)

    override fun executar(consulta: ListarCategoriasQuery): List<CategoriaDeInsumo> =
        repositorio.listar(consulta.termo?.trim()?.ifBlank { null }, consulta.incluirDesativadas)
            .sortedWith(compareBy(OrdemDeExibicao.porNome()) { it.nome })
}
