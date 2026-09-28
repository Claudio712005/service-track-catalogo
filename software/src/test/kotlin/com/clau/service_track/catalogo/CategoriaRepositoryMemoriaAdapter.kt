package com.clau.service_track.catalogo

import com.clau.service_track.catalogo.application.port.out.repository.CategoriaRepositoryPort
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
@Profile("teste")
class CategoriaRepositoryMemoriaAdapter : CategoriaRepositoryPort {

    private val acervo = ConcurrentHashMap<String, CategoriaDeInsumo>()

    init {
        reiniciar()
    }

    fun reiniciar() {
        acervo.clear()
        catalogoInicial().forEach { acervo[it.id.value] = it }
    }

    override fun salvar(categoria: CategoriaDeInsumo): CategoriaDeInsumo {
        acervo[categoria.id.value] = categoria
        return categoria
    }

    override fun buscarPorId(id: DomainId): CategoriaDeInsumo? = acervo[id.value]

    override fun buscarPorIds(ids: Collection<DomainId>): Map<DomainId, CategoriaDeInsumo> = ids
        .mapNotNull { acervo[it.value] }
        .associateBy { it.id }

    override fun buscarPorCodigo(codigo: String): CategoriaDeInsumo? = acervo.values
        .firstOrNull { it.codigo.equals(codigo.trim(), ignoreCase = true) }

    override fun listar(termo: String?, incluirDesativadas: Boolean): List<CategoriaDeInsumo> = acervo.values
        .filter { incluirDesativadas || it.ativa }
        .filter { categoria ->
            termo == null ||
                categoria.codigo.contains(termo, ignoreCase = true) ||
                categoria.nome.contains(termo, ignoreCase = true)
        }
        .toList()

    fun desativadas(): Set<DomainId> = acervo.values.filterNot { it.ativa }.map { it.id }.toSet()

    override fun existeComCodigo(codigo: String): Boolean = buscarPorCodigo(codigo) != null

    private fun catalogoInicial(): List<CategoriaDeInsumo> = listOf(
        CategoriaDeInsumo.reconstituir(
            id = DomainId.de(OLEO_MOTOR),
            codigo = "OLEO_MOTOR",
            nome = "Óleo de motor",
            unidadePadrao = UnidadeDeMedida.LITRO,
            atributos = listOf(
                DefinicaoDeAtributo(
                    chave = "viscosidade",
                    rotulo = "Viscosidade",
                    tipo = TipoDeAtributo.OPCAO,
                    obrigatorio = true,
                    opcoes = listOf("0W20", "5W30", "10W40", "15W40"),
                ),
                DefinicaoDeAtributo(
                    chave = "especificacao-api",
                    rotulo = "Especificação API",
                    tipo = TipoDeAtributo.TEXTO,
                    obrigatorio = true,
                ),
                DefinicaoDeAtributo(
                    chave = "sintetico",
                    rotulo = "Sintético",
                    tipo = TipoDeAtributo.BOOLEANO,
                ),
            ),
        ),
        CategoriaDeInsumo.reconstituir(
            id = DomainId.de(PNEU),
            codigo = "PNEU",
            nome = "Pneu",
            unidadePadrao = UnidadeDeMedida.UNIDADE,
            atributos = listOf(
                DefinicaoDeAtributo(
                    chave = "aro",
                    rotulo = "Aro",
                    tipo = TipoDeAtributo.INTEIRO,
                    unidade = "pol",
                    obrigatorio = true,
                ),
            ),
        ),
    )

    companion object {
        const val OLEO_MOTOR = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11"
        const val PNEU = "018f2ca1-2b77-7f10-8c02-91ab7d4e5f20"
    }
}
