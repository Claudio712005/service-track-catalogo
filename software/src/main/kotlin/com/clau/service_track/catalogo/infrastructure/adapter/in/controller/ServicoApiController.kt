package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.application.port.`in`.api.IServicoApiPort
import com.clau.service_track.catalogo.application.port.`in`.api.dto.AtualizarServicoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarServicoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.ServicoResponse
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.BuscarServicoQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.BuscarServicoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.CriarServicoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.DesativarServicoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.DesativarServicoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.ListarServicosQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.ListarServicosUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.AtualizarServicoUseCase
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper.ServicoWebMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class ServicoApiController(
    private val criarServico: CriarServicoUseCase,
    private val atualizarServico: AtualizarServicoUseCase,
    private val desativarServico: DesativarServicoUseCase,
    private val buscarServico: BuscarServicoUseCase,
    private val listarServicos: ListarServicosUseCase,
    private val mapper: ServicoWebMapper,
) : IServicoApiPort {

    override fun listarServicos(incluirInativos: Boolean): List<ServicoResponse> =
        mapper.paraResposta(listarServicos.executar(ListarServicosQuery(incluirInativos)))

    override fun buscarServicoPorId(id: String): ServicoResponse =
        mapper.paraResposta(buscarServico.executar(BuscarServicoQuery(mapper.paraIdentificador(id))))

    override fun criarServico(requisicao: CriarServicoRequest): ResponseEntity<ServicoResponse> {
        val criado = criarServico.executar(mapper.paraComando(requisicao))
        val endereco = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(criado.id.value)
            .toUri()
        return ResponseEntity.created(endereco).body(mapper.paraResposta(criado))
    }

    override fun atualizarServico(id: String, requisicao: AtualizarServicoRequest): ServicoResponse =
        mapper.paraResposta(atualizarServico.executar(mapper.paraComando(id, requisicao)))

    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun desativarServico(id: String) {
        desativarServico.executar(DesativarServicoCommand(mapper.paraIdentificador(id)))
    }
}
