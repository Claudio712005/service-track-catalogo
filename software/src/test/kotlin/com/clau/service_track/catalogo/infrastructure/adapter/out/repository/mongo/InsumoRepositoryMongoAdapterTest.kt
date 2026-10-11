package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.mongo

import com.clau.service_track.catalogo.application.port.out.repository.FiltroDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.CategoriaPersistenceMapper
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.InsumoPersistenceMapper
import com.clau.service_track.catalogo.infrastructure.entity.mongo.CategoriaDocumento
import com.clau.service_track.catalogo.infrastructure.entity.mongo.InsumoDocumento
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query

class InsumoRepositoryMongoAdapterTest {

    private val insumos: InsumoMongoRepository = mock(InsumoMongoRepository::class.java)
    private val categorias: CategoriaMongoRepository = mock(CategoriaMongoRepository::class.java)
    private val template: MongoTemplate = mock(MongoTemplate::class.java)

    private val adaptador = InsumoRepositoryMongoAdapter(
        insumos,
        categorias,
        InsumoPersistenceMapper(),
        CategoriaPersistenceMapper(),
        template,
    )

    private val momento: Instant = Instant.parse("2026-02-10T08:00:00Z")
    private val categoriaId = UUID.randomUUID().toString()

    private fun documento(
        id: String = UUID.randomUUID().toString(),
        sku: String = "OL-5W30-SIN-1L",
        categoria: String = categoriaId,
        ativo: Boolean = true,
    ) = InsumoDocumento(
        id = id,
        categoriaId = categoria,
        sku = sku,
        nome = "Oleo 5W30",
        unidadeMedida = UnidadeDeMedida.LITRO,
        custoPadrao = BigDecimal("38.50"),
        ativo = ativo,
        dataCriacao = momento,
        dataAtualizacao = momento,
    )

    private fun categoriaDocumento(id: String = categoriaId, ativa: Boolean = true) = CategoriaDocumento(
        id = id,
        codigo = "LUBRIFICANTES",
        nome = "Lubrificantes",
        unidadePadrao = UnidadeDeMedida.LITRO,
        ativa = ativa,
        dataCriacao = momento,
        dataAtualizacao = momento,
    )

    private fun consultaDe(filtro: FiltroDeInsumo, achados: List<InsumoDocumento> = emptyList()): Query {
        org.mockito.Mockito
            .`when`(template.find(any(Query::class.java), org.mockito.ArgumentMatchers.eq(InsumoDocumento::class.java)))
            .thenReturn(achados)

        adaptador.listar(filtro)

        val capturada = ArgumentCaptor.forClass(Query::class.java)
        verify(template).find(capturada.capture(), org.mockito.ArgumentMatchers.eq(InsumoDocumento::class.java))
        return capturada.value
    }

    @Test
    fun `buscar por sku inexistente devolve nulo`() {
        org.mockito.Mockito.`when`(insumos.findBySku("NAO-EXISTE")).thenReturn(null)

        assertNull(adaptador.buscarPorSku("NAO-EXISTE"))
    }

    @Test
    fun `buscar por sku traz o insumo com a categoria resolvida`() {
        val doc = documento()
        org.mockito.Mockito.`when`(insumos.findBySku(doc.sku)).thenReturn(doc)
        org.mockito.Mockito.`when`(categorias.findById(categoriaId)).thenReturn(Optional.of(categoriaDocumento()))

        val achado = adaptador.buscarPorSku(doc.sku)

        assertEquals(doc.sku, achado?.sku)
        assertEquals(DomainId.de(categoriaId), achado?.categoriaId)
    }

    @Test
    fun `insumo de categoria que nao existe mais ainda e legivel`() {
        val doc = documento()
        org.mockito.Mockito.`when`(insumos.findById(doc.id)).thenReturn(Optional.of(doc))
        org.mockito.Mockito.`when`(categorias.findById(categoriaId)).thenReturn(Optional.empty())

        val achado = adaptador.buscarPorId(DomainId.de(doc.id))

        assertEquals(doc.sku, achado?.sku, "categoria ausente nao pode esconder o insumo")
    }

    @Test
    fun `buscar por codigo de barras usa a consulta propria`() {
        val doc = documento()
        org.mockito.Mockito.`when`(insumos.findByCodigoBarras("7891234567890")).thenReturn(doc)
        org.mockito.Mockito.`when`(categorias.findById(categoriaId)).thenReturn(Optional.of(categoriaDocumento()))

        assertEquals(doc.sku, adaptador.buscarPorCodigoBarras("7891234567890")?.sku)
    }

    @Test
    fun `existe com sku e contagem por categoria delegam para a colecao`() {
        org.mockito.Mockito.`when`(insumos.existsBySku("OL-5W30-SIN-1L")).thenReturn(true)
        org.mockito.Mockito.`when`(insumos.countByCategoriaId(categoriaId)).thenReturn(3L)

        assertTrue(adaptador.existeComSku("OL-5W30-SIN-1L"))
        assertFalse(adaptador.existeComSku("OUTRO"))
        assertEquals(3L, adaptador.contarPorCategoria(DomainId.de(categoriaId)))
    }

    @Test
    fun `salvar insumo existente atualiza o documento em maos, sem criar outro`() {
        val existente = documento()
        existente.descricao = "descricao antiga"
        org.mockito.Mockito.`when`(insumos.findById(existente.id)).thenReturn(Optional.of(existente))
        org.mockito.Mockito.doAnswer { it.arguments[0] }
            .`when`(insumos)
            .save(any(InsumoDocumento::class.java))
        org.mockito.Mockito.`when`(categorias.findById(categoriaId)).thenReturn(Optional.of(categoriaDocumento()))

        val dominio = adaptador.buscarPorId(DomainId.de(existente.id))!!
        dominio.atualizarDescricao("descricao nova")

        adaptador.salvar(dominio)

        val capturado = ArgumentCaptor.forClass(InsumoDocumento::class.java)
        verify(insumos).save(capturado.capture())

        assertTrue(
            capturado.value === existente,
            "salvar insumo existente tem de reaproveitar o documento lido, nao criar um segundo",
        )
        assertEquals("descricao nova", capturado.value.descricao)
        assertEquals(existente.dataCriacao, capturado.value.dataCriacao)
    }

    @Test
    fun `salvar insumo novo grava um documento montado do zero`() {
        val novo = documento()
        org.mockito.Mockito.`when`(insumos.findById(novo.id)).thenReturn(Optional.empty())
        org.mockito.Mockito.`when`(categorias.findById(categoriaId)).thenReturn(Optional.of(categoriaDocumento()))
        org.mockito.Mockito.doAnswer { it.arguments[0] }
            .`when`(insumos)
            .save(any(InsumoDocumento::class.java))

        org.mockito.Mockito.`when`(insumos.findBySku(novo.sku)).thenReturn(novo)
        val dominio = adaptador.buscarPorSku(novo.sku)!!

        org.mockito.Mockito.reset(insumos)
        org.mockito.Mockito.`when`(insumos.findById(novo.id)).thenReturn(Optional.empty())
        org.mockito.Mockito.doAnswer { it.arguments[0] }
            .`when`(insumos)
            .save(any(InsumoDocumento::class.java))

        val salvo = adaptador.salvar(dominio)

        assertEquals(novo.sku, salvo.sku)
        assertEquals(DomainId.de(novo.id), salvo.id)
    }

    @Test
    fun `listar sem filtro nenhum esconde inativo e insumo de categoria desativada`() {
        org.mockito.Mockito.`when`(categorias.findAllByAtiva(false))
            .thenReturn(listOf(categoriaDocumento(id = "categoria-morta", ativa = false)))

        val consulta = consultaDe(FiltroDeInsumo())
        val criterio = consulta.queryObject.toJson()

        assertTrue(criterio.contains("ATIVO"), "sem incluirInativos o filtro tem de exigir ATIVO: $criterio")
        assertTrue(
            criterio.contains("CATEGORIA_ID") && criterio.contains("categoria-morta"),
            "insumo de categoria desativada tem de sair da listagem: $criterio",
        )
    }

    @Test
    fun `sem categoria desativada o filtro nao inclui clausula de categoria`() {
        org.mockito.Mockito.`when`(categorias.findAllByAtiva(false)).thenReturn(emptyList())

        val criterio = consultaDe(FiltroDeInsumo()).queryObject.toJson()

        assertTrue(criterio.contains("ATIVO"))
        assertFalse(
            criterio.contains("CATEGORIA_ID"),
            "sem categoria desativada nao ha o que excluir, e a clausula nao deve aparecer: $criterio",
        )
    }

    @Test
    fun `incluirInativos derruba as duas exclusoes`() {
        val consulta = consultaDe(FiltroDeInsumo(incluirInativos = true))

        assertEquals("{}", consulta.queryObject.toJson(), "com incluirInativos a consulta nao filtra nada")
        verify(categorias, never()).findAllByAtiva(false)
    }

    @Test
    fun `filtro por categoria entra na consulta`() {
        org.mockito.Mockito.`when`(categorias.findAllByAtiva(false)).thenReturn(emptyList())

        val criterio = consultaDe(
            FiltroDeInsumo(categoriaId = DomainId.de(categoriaId))
        ).queryObject.toJson()

        assertTrue(criterio.contains(categoriaId), "a categoria pedida tem de aparecer na consulta: $criterio")
    }

    @Test
    fun `termo de busca vira regex escapada em sku, nome e marca`() {
        org.mockito.Mockito.`when`(categorias.findAllByAtiva(false)).thenReturn(emptyList())

        val criterio = consultaDe(FiltroDeInsumo(termo = "5W30.*")).queryObject.toJson()

        assertTrue(criterio.contains("SKU"))
        assertTrue(criterio.contains("NOME"))
        assertTrue(criterio.contains("MARCA"))
        assertTrue(
            criterio.contains("\\\\Q") || criterio.contains("\\\\Q5W30"),
            "o termo tem de ser escapado, senao '.*' do usuario vira curinga: $criterio",
        )
    }

    @Test
    fun `listar resolve as categorias numa consulta so, nao uma por insumo`() {
        val primeiro = documento(sku = "OL-5W30")
        val segundo = documento(sku = "OL-20W50")
        org.mockito.Mockito.`when`(categorias.findAllByAtiva(false)).thenReturn(emptyList())
        org.mockito.Mockito.`when`(categorias.findAllById(listOf(categoriaId)))
            .thenReturn(listOf(categoriaDocumento()))

        val achados = run {
            org.mockito.Mockito
                .`when`(template.find(any(Query::class.java), org.mockito.ArgumentMatchers.eq(InsumoDocumento::class.java)))
                .thenReturn(listOf(primeiro, segundo))
            adaptador.listar(FiltroDeInsumo())
        }

        assertEquals(listOf("OL-5W30", "OL-20W50"), achados.map { it.sku })
        verify(categorias).findAllById(anyIterable())
        verify(categorias, never()).findById(categoriaId)
    }

    @Test
    fun `listagem vazia nao consulta categoria nenhuma`() {
        org.mockito.Mockito.`when`(categorias.findAllByAtiva(false)).thenReturn(emptyList())

        val achados = run {
            org.mockito.Mockito
                .`when`(template.find(any(Query::class.java), org.mockito.ArgumentMatchers.eq(InsumoDocumento::class.java)))
                .thenReturn(emptyList())
            adaptador.listar(FiltroDeInsumo())
        }

        assertTrue(achados.isEmpty())
        verify(categorias, never()).findAllById(anyIterable())
    }
}
