package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.application.port.`in`.api.ICategoriaApiPort
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CategoriaResponse
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarCategoriaRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.DefinicaoDeAtributoRequest
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AcrescentarAtributoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.BuscarCategoriaQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.BuscarCategoriaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.CriarCategoriaUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.ListarCategoriasQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.ListarCategoriasUseCase
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper.CategoriaWebMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class CategoriaApiController(
    private val criarCategoria: CriarCategoriaUseCase,
    private val acrescentarAtributo: AcrescentarAtributoUseCase,
    private val buscarCategoria: BuscarCategoriaUseCase,
    private val listarCategorias: ListarCategoriasUseCase,
    private val mapper: CategoriaWebMapper,
) : ICategoriaApiPort {

    override fun listarCategorias(termo: String?): List<CategoriaResponse> =
        mapper.paraResposta(listarCategorias.executar(ListarCategoriasQuery(termo)))

    override fun buscarCategoriaPorId(id: String): CategoriaResponse =
        mapper.paraResposta(buscarCategoria.executar(BuscarCategoriaQuery(mapper.paraIdentificador(id))))

    override fun criarCategoria(requisicao: CriarCategoriaRequest): ResponseEntity<CategoriaResponse> {
        val criada = criarCategoria.executar(mapper.paraComando(requisicao))
        val endereco = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(criada.id.value)
            .toUri()
        return ResponseEntity.created(endereco).body(mapper.paraResposta(criada))
    }

    override fun acrescentarAtributo(id: String, requisicao: DefinicaoDeAtributoRequest): CategoriaResponse =
        mapper.paraResposta(acrescentarAtributo.executar(mapper.paraComando(id, requisicao)))
}
