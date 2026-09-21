package com.clau.service_track.catalogo.application.handler.servico

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.AtualizarServicoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.AtualizarServicoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.CriarServicoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.CriarServicoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.DesativarServicoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.DesativarServicoUseCase
import com.clau.service_track.catalogo.application.port.out.repository.ServicoRepositoryPort
import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.shared.annotation.UseCase

@UseCase
class ServicoCommandHandler(
    private val repositorio: ServicoRepositoryPort,
) : CriarServicoUseCase, AtualizarServicoUseCase, DesativarServicoUseCase {

    override fun executar(comando: CriarServicoCommand): Servico {
        if (repositorio.existeComNome(comando.nome)) {
            throw ConflitoDeEstadoException("Já existe um serviço cadastrado com o nome '${comando.nome}'")
        }

        val servico = Servico.criar(
            nome = comando.nome,
            descricao = comando.descricao,
            valorReferencia = comando.valorReferencia,
        )
        return repositorio.salvar(servico)
    }

    override fun executar(comando: AtualizarServicoCommand): Servico {
        val servico = exigirServico(comando.id)

        if (!servico.ativo) {
            throw ConflitoDeEstadoException("Serviço '${servico.nome}' está desativado e não pode ser alterado")
        }

        servico.atualizarDescricao(comando.descricao)
        comando.valorReferencia?.let { servico.atualizarValorReferencia(it) }

        return repositorio.salvar(servico)
    }

    override fun executar(comando: DesativarServicoCommand) {
        val servico = exigirServico(comando.id)
        servico.desativar()
        repositorio.salvar(servico)
    }

    private fun exigirServico(id: DomainId): Servico =
        repositorio.buscarPorId(id) ?: throw RecursoNaoEncontradoException("Serviço", id.value)
}
