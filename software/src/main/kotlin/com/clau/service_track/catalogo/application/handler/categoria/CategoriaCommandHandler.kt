package com.clau.service_track.catalogo.application.handler.categoria

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AcrescentarAtributoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AcrescentarAtributoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AlternarCategoriaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.categoria.AlternarCategoriaUseCase
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
) : CriarCategoriaUseCase, AcrescentarAtributoUseCase, AlternarCategoriaUseCase {

    private val log = LoggerFactory.getLogger(CategoriaCommandHandler::class.java)

    override fun executar(comando: CriarCategoriaCommand): CategoriaDeInsumo {
        val codigo = comando.codigo.trim().uppercase()

        repositorio.buscarPorCodigo(codigo)?.let { existente ->
            log.warn(
                "categoria recusada: codigo ja cadastrado codigo={} ativa={}",
                codigo, existente.ativa,
            )
            if (existente.ativa) {
                throw ConflitoDeEstadoException("Já existe uma categoria ativa com o código '$codigo'")
            }
            throw ConflitoDeEstadoException(
                "Já existe uma categoria com o código '$codigo', atualmente desativada. " +
                    "Reative-a em PUT /categorias/${existente.id.value}/ativacao em vez de cadastrar outra"
            )
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

    override fun executar(comando: AlternarCategoriaCommand): CategoriaDeInsumo {
        val categoria = repositorio.buscarPorId(comando.id)
            ?: throw RecursoNaoEncontradoException("Categoria", comando.id.value)

        if (comando.ativa) categoria.reativar() else categoria.desativar()

        val salva = repositorio.salvar(categoria)
        log.info(
            "categoria {} categoriaId={} codigo={}",
            if (salva.ativa) "reativada" else "desativada", salva.id.value, salva.codigo,
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
