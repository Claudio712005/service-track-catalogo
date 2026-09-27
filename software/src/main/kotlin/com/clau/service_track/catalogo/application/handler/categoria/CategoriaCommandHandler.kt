package com.clau.service_track.catalogo.application.handler.categoria

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AcrescentarAtributoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AcrescentarAtributoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.CriarCategoriaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.CriarCategoriaUseCase
import com.clau.service_track.catalogo.application.port.out.repository.CategoriaRepositoryPort
import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.shared.annotation.UseCase
import org.slf4j.LoggerFactory

@UseCase
class CategoriaCommandHandler(
    private val repositorio: CategoriaRepositoryPort,
) : CriarCategoriaUseCase, AcrescentarAtributoUseCase {

    private val log = LoggerFactory.getLogger(CategoriaCommandHandler::class.java)

    override fun executar(comando: CriarCategoriaCommand): CategoriaDeInsumo {
        val codigo = comando.codigo.trim().uppercase()

        if (repositorio.existeComCodigo(codigo)) {
            log.warn("categoria recusada: codigo ja cadastrado codigo={}", codigo)
            throw ConflitoDeEstadoException("Já existe uma categoria com o código '$codigo'")
        }

        val categoria = CategoriaDeInsumo.criar(
            codigo = codigo,
            nome = comando.nome.trim(),
            unidadePadrao = comando.unidadePadrao,
            atributos = comando.atributos,
        )

        val salva = repositorio.salvar(categoria)
        log.info(
            "categoria cadastrada categoriaId={} codigo={} atributos={}",
            salva.id.value, salva.codigo, salva.atributos.size,
        )
        return salva
    }

    override fun executar(comando: AcrescentarAtributoCommand): CategoriaDeInsumo {
        val categoria = repositorio.buscarPorId(comando.categoriaId)
            ?: throw RecursoNaoEncontradoException("Categoria", comando.categoriaId.value)

        categoria.acrescentarAtributo(comando.definicao)

        val salva = repositorio.salvar(categoria)
        log.info(
            "atributo acrescentado categoriaId={} codigo={} atributo={} tipo={}",
            salva.id.value, salva.codigo, comando.definicao.chave, comando.definicao.tipo,
        )
        return salva
    }
}
