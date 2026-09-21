package com.clau.service_track.catalogo.application.handler.servico

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.BuscarServicoQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.BuscarServicoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.ListarServicosQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.ListarServicosUseCase
import com.clau.service_track.catalogo.application.port.out.repository.ServicoRepositoryPort
import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.shared.annotation.UseCase

@UseCase
class ServicoQueryHandler(
    private val repositorio: ServicoRepositoryPort,
) : BuscarServicoUseCase, ListarServicosUseCase {

    override fun executar(consulta: BuscarServicoQuery): Servico =
        repositorio.buscarPorId(consulta.id)
            ?: throw RecursoNaoEncontradoException("Serviço", consulta.id.value)

    override fun executar(consulta: ListarServicosQuery): List<Servico> =
        repositorio.listar(consulta.incluirInativos).sortedBy { it.nome }
}
