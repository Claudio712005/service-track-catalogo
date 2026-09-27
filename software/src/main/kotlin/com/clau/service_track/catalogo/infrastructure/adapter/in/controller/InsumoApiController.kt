package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.application.port.`in`.api.IInsumoApiPort
import com.clau.service_track.catalogo.application.port.`in`.api.dto.AtualizarInsumoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarInsumoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.InsumoResponse
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.AtualizarInsumoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.BuscarInsumoQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.BuscarInsumoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.CriarInsumoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.DesativarInsumoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.DesativarInsumoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.ListarInsumosQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.ListarInsumosUseCase
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper.CategoriaWebMapper
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper.InsumoWebMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class InsumoApiController(
    private val criarInsumo: CriarInsumoUseCase,
    private val atualizarInsumo: AtualizarInsumoUseCase,
    private val desativarInsumo: DesativarInsumoUseCase,
    private val buscarInsumo: BuscarInsumoUseCase,
    private val listarInsumos: ListarInsumosUseCase,
    private val mapper: InsumoWebMapper,
    private val identificadores: CategoriaWebMapper,
) : IInsumoApiPort {

    override fun listarInsumos(
        incluirInativos: Boolean,
        categoriaId: String?,
        termo: String?,
    ): List<InsumoResponse> = mapper.paraResposta(
        listarInsumos.executar(
            ListarInsumosQuery(
                incluirInativos = incluirInativos,
                categoriaId = categoriaId?.let(identificadores::paraIdentificador),
                termo = termo,
            )
        )
    )

    override fun buscarInsumoPorId(id: String): InsumoResponse = mapper.paraResposta(
        buscarInsumo.executar(BuscarInsumoQuery(id = identificadores.paraIdentificador(id)))
    )

    override fun buscarInsumoPorSku(sku: String): InsumoResponse = mapper.paraResposta(
        buscarInsumo.executar(BuscarInsumoQuery(sku = sku))
    )

    override fun criarInsumo(requisicao: CriarInsumoRequest): ResponseEntity<InsumoResponse> {
        val criado = criarInsumo.executar(mapper.paraComando(requisicao))
        val endereco = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(criado.id.value)
            .toUri()
        return ResponseEntity.created(endereco).body(mapper.paraResposta(criado))
    }

    override fun atualizarInsumo(id: String, requisicao: AtualizarInsumoRequest): InsumoResponse =
        mapper.paraResposta(atualizarInsumo.executar(mapper.paraComando(id, requisicao)))

    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun desativarInsumo(id: String) {
        desativarInsumo.executar(DesativarInsumoCommand(identificadores.paraIdentificador(id)))
    }
}
