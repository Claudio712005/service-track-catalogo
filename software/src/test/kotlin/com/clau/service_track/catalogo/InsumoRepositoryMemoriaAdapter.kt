package com.clau.service_track.catalogo

import com.clau.service_track.catalogo.application.port.out.repository.FiltroDeInsumo
import com.clau.service_track.catalogo.application.port.out.repository.InsumoRepositoryPort
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.Especificacao
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorDeAtributo
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Repository
@Profile("teste")
class InsumoRepositoryMemoriaAdapter(
    private val categorias: CategoriaRepositoryMemoriaAdapter,
) : InsumoRepositoryPort {

    private val acervo = ConcurrentHashMap<String, Insumo>()

    init {
        reiniciar()
    }

    fun reiniciar() {
        acervo.clear()
        catalogoInicial().forEach { acervo[it.id.value] = it }
    }

    override fun salvar(insumo: Insumo): Insumo {
        acervo[insumo.id.value] = insumo
        return insumo
    }

    override fun buscarPorId(id: DomainId): Insumo? = acervo[id.value]

    override fun buscarPorSku(sku: String): Insumo? = acervo.values
        .firstOrNull { it.sku.equals(sku.trim(), ignoreCase = true) }

    override fun buscarPorCodigoBarras(codigoBarras: String): Insumo? = acervo.values
        .firstOrNull { it.codigoBarras == codigoBarras.trim() }

    override fun listar(filtro: FiltroDeInsumo): List<Insumo> {
        val desativadas = if (filtro.incluirInativos) emptySet() else categorias.desativadas()

        return acervo.values
        .filter { filtro.incluirInativos || it.ativo }
        .filter { filtro.incluirInativos || it.categoriaId !in desativadas }
        .filter { filtro.categoriaId == null || it.categoriaId == filtro.categoriaId }
        .filter { insumo ->
            val termo = filtro.termo ?: return@filter true
            insumo.sku.contains(termo, ignoreCase = true) ||
                insumo.nome.contains(termo, ignoreCase = true) ||
                (insumo.marca?.contains(termo, ignoreCase = true) ?: false)
        }
        .toList()
    }

    override fun existeComSku(sku: String): Boolean = buscarPorSku(sku) != null

    override fun contarPorCategoria(categoriaId: DomainId): Long = acervo.values
        .count { it.categoriaId == categoriaId }
        .toLong()

    private fun catalogoInicial(): List<Insumo> = listOf(
        Insumo.reconstituir(
            id = DomainId.de(OLEO_5W30),
            categoriaId = DomainId.de(CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR),
            sku = "OL-5W30-SYN-1L",
            nome = "Óleo 5W30 sintético 1L",
            descricao = "Óleo lubrificante sintético para motores a gasolina e flex, embalagem de 1 litro.",
            unidadeDeMedida = UnidadeDeMedida.LITRO,
            custo = ValorMonetario.de("38.90"),
            especificacao = Especificacao.reconstituir(
                mapOf(
                    "viscosidade" to ValorDeAtributo.Opcao("5W30"),
                    "especificacao-api" to ValorDeAtributo.Texto("SN"),
                    "sintetico" to ValorDeAtributo.Booleano(true),
                )
            ),
            dataCriacao = LocalDateTime.of(2026, 9, 1, 8, 30),
            dataAtualizacao = LocalDateTime.of(2026, 9, 1, 8, 30),
            marca = "Lubrax",
            fabricante = "Petrobras",
            codigoFabricante = "LB-5W30-1L",
            codigoBarras = "7891234567890",
            controlaLote = true,
            validadeEmDias = 730,
        ),
        Insumo.reconstituir(
            id = DomainId.de(PNEU_ARO16),
            categoriaId = DomainId.de(CategoriaRepositoryMemoriaAdapter.PNEU),
            sku = "PN-205-55-R16",
            nome = "Pneu 205/55 R16",
            descricao = "Pneu radial para automóvel de passeio.",
            unidadeDeMedida = UnidadeDeMedida.UNIDADE,
            custo = ValorMonetario.de("459.90"),
            especificacao = Especificacao.reconstituir(mapOf("aro" to ValorDeAtributo.Inteiro(16))),
            dataCriacao = LocalDateTime.of(2026, 9, 2, 9, 0),
            dataAtualizacao = LocalDateTime.of(2026, 9, 2, 9, 0),
            marca = "Pirelli",
        ),
        Insumo.reconstituir(
            id = DomainId.de(FILTRO_DESCONTINUADO),
            categoriaId = DomainId.de(CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR),
            sku = "OL-20W50-MIN-1L",
            nome = "Óleo 20W50 mineral 1L",
            descricao = "Linha descontinuada pelo fabricante.",
            unidadeDeMedida = UnidadeDeMedida.LITRO,
            custo = ValorMonetario.de("24.50"),
            especificacao = Especificacao.reconstituir(
                mapOf(
                    "viscosidade" to ValorDeAtributo.Opcao("15W40"),
                    "especificacao-api" to ValorDeAtributo.Texto("SL"),
                )
            ),
            dataCriacao = LocalDateTime.of(2026, 2, 10, 14, 0),
            dataAtualizacao = LocalDateTime.of(2026, 8, 20, 11, 0),
            ativo = false,
        ),
    )

    companion object {
        const val OLEO_5W30 = "018f30bb-77a1-7c22-9b10-2a44de81f0aa"
        const val PNEU_ARO16 = "018f30c4-1d55-7a98-8f03-7bb1c2e4d5f6"
        const val FILTRO_DESCONTINUADO = "018f30d0-9e11-7b44-9c55-3ad2f1b0e7c8"
    }
}
