package com.clau.service_track.catalogo.application.handler.insumo

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.AtualizarInsumoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.AtualizarInsumoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.CriarInsumoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.CriarInsumoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.DesativarInsumoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.insumo.DesativarInsumoUseCase
import com.clau.service_track.catalogo.application.port.out.repository.CategoriaRepositoryPort
import com.clau.service_track.catalogo.application.port.out.repository.InsumoRepositoryPort
import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.shared.annotation.UseCase
import org.slf4j.LoggerFactory

@UseCase
class InsumoCommandHandler(
    private val insumos: InsumoRepositoryPort,
    private val categorias: CategoriaRepositoryPort,
) : CriarInsumoUseCase, AtualizarInsumoUseCase, DesativarInsumoUseCase {

    private val log = LoggerFactory.getLogger(InsumoCommandHandler::class.java)

    override fun executar(comando: CriarInsumoCommand): Insumo {
        val categoria = exigirCategoria(comando.categoriaId)
        val sku = comando.sku.trim().uppercase()

        if (insumos.existeComSku(sku)) {
            log.warn("insumo recusado: sku ja cadastrado sku={}", sku)
            throw ConflitoDeEstadoException("Já existe um insumo cadastrado com o SKU '$sku'")
        }

        comando.codigoBarras?.trim()?.takeIf { it.isNotBlank() }?.let { codigo ->
            if (insumos.buscarPorCodigoBarras(codigo) != null) {
                log.warn("insumo recusado: codigo de barras ja cadastrado sku={}", sku)
                throw ConflitoDeEstadoException("Já existe um insumo com o código de barras informado")
            }
        }

        val insumo = Insumo.criar(
            categoria = categoria,
            sku = sku,
            nome = comando.nome,
            descricao = comando.descricao,
            custo = comando.custo,
            especificacao = comando.especificacao,
            unidadeDeMedida = comando.unidadeDeMedida ?: categoria.unidadePadrao,
            marca = comando.marca,
            fabricante = comando.fabricante,
            codigoFabricante = comando.codigoFabricante,
            codigoBarras = comando.codigoBarras,
            controlaLote = comando.controlaLote,
            validadeEmDias = comando.validadeEmDias,
        )

        val salvo = insumos.salvar(insumo)
        log.info(
            "insumo cadastrado insumoId={} sku={} categoria={} unidade={} atributos={}",
            salvo.id.value, salvo.sku, categoria.codigo, salvo.unidadeDeMedida, salvo.especificacao.comoMapa().size,
        )
        return salvo
    }

    override fun executar(comando: AtualizarInsumoCommand): Insumo {
        val insumo = exigirInsumo(comando.id)
        val categoria = exigirCategoria(insumo.categoriaId)

        comando.codigoBarras?.trim()?.takeIf { it.isNotBlank() }?.let { codigo ->
            val dono = insumos.buscarPorCodigoBarras(codigo)
            if (dono != null && dono.id != insumo.id) {
                log.warn("atualizacao recusada: codigo de barras em uso insumoId={}", insumo.id.value)
                throw ConflitoDeEstadoException("Código de barras informado já pertence a outro insumo")
            }
        }

        insumo.atualizarDescricao(comando.descricao)
        insumo.atualizarCusto(comando.custo)
        insumo.atualizarIdentificacaoComercial(
            marca = comando.marca,
            fabricante = comando.fabricante,
            codigoFabricante = comando.codigoFabricante,
            codigoBarras = comando.codigoBarras,
        )
        insumo.definirControleDeLote(comando.controlaLote, comando.validadeEmDias)
        insumo.reespecificar(categoria, comando.especificacao)

        val salvo = insumos.salvar(insumo)
        log.info(
            "insumo atualizado insumoId={} sku={} atributos={}",
            salvo.id.value, salvo.sku, salvo.especificacao.comoMapa().size,
        )
        return salvo
    }

    override fun executar(comando: DesativarInsumoCommand) {
        val insumo = exigirInsumo(comando.id)
        insumo.desativar()
        insumos.salvar(insumo)
        log.info("insumo desativado insumoId={} sku={}", insumo.id.value, insumo.sku)
    }

    private fun exigirInsumo(id: DomainId): Insumo =
        insumos.buscarPorId(id) ?: throw RecursoNaoEncontradoException("Insumo", id.value)

    private fun exigirCategoria(id: DomainId): CategoriaDeInsumo =
        categorias.buscarPorId(id) ?: throw RecursoNaoEncontradoException("Categoria", id.value)
}
