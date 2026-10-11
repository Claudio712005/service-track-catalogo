package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.mongo

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.CategoriaPersistenceMapper
import com.clau.service_track.catalogo.infrastructure.entity.mongo.CategoriaDocumento
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class CategoriaRepositoryMongoAdapterTest {

    private val colecao: CategoriaMongoRepository = mock(CategoriaMongoRepository::class.java)
    private val mapper = CategoriaPersistenceMapper()
    private val adaptador = CategoriaRepositoryMongoAdapter(colecao, mapper)

    private val criacaoAntiga: Instant = Instant.parse("2026-01-05T10:00:00Z")

    private fun categoria(
        id: String = UUID.randomUUID().toString(),
        codigo: String = "LUBRIFICANTES",
        ativa: Boolean = true,
    ) = CategoriaDeInsumo.reconstituir(
        id = DomainId.de(id),
        codigo = codigo,
        nome = "Lubrificantes",
        unidadePadrao = UnidadeDeMedida.LITRO,
        atributos = emptyList(),
        ativa = ativa,
    )

    private fun documento(
        id: String = UUID.randomUUID().toString(),
        codigo: String = "LUBRIFICANTES",
        ativa: Boolean = true,
    ) = CategoriaDocumento(
        id = id,
        codigo = codigo,
        nome = "Lubrificantes",
        unidadePadrao = UnidadeDeMedida.LITRO,
        ativa = ativa,
        dataCriacao = criacaoAntiga,
        dataAtualizacao = criacaoAntiga,
    )

    private fun devolveOQueRecebe() {
        org.mockito.Mockito.doAnswer { it.arguments[0] }
            .`when`(colecao)
            .save(org.mockito.ArgumentMatchers.any(CategoriaDocumento::class.java))
    }

    @Test
    fun `salvar categoria nova grava o que o dominio tem`() {
        val nova = categoria()
        org.mockito.Mockito.`when`(colecao.findById(nova.id.value)).thenReturn(Optional.empty())
        devolveOQueRecebe()

        val salva = adaptador.salvar(nova)

        assertEquals(nova.id, salva.id)
        assertEquals(nova.codigo, salva.codigo)
        assertEquals(nova.unidadePadrao, salva.unidadePadrao)
    }

    @Test
    fun `salvar categoria existente preserva a data de criacao original`() {
        val existente = documento()
        org.mockito.Mockito.`when`(colecao.findById(existente.id)).thenReturn(Optional.of(existente))
        devolveOQueRecebe()

        adaptador.salvar(categoria(id = existente.id))

        val capturado = ArgumentCaptor.forClass(CategoriaDocumento::class.java)
        verify(colecao).save(capturado.capture())

        assertEquals(
            criacaoAntiga,
            capturado.value.dataCriacao,
            "regravar categoria nao pode reescrever a data de criacao",
        )
        assertTrue(
            capturado.value.dataAtualizacao.isAfter(criacaoAntiga),
            "a data de atualizacao tem de avancar",
        )
    }

    @Test
    fun `buscar por identificador inexistente devolve nulo`() {
        val id = UUID.randomUUID().toString()
        org.mockito.Mockito.`when`(colecao.findById(id)).thenReturn(Optional.empty())

        assertNull(adaptador.buscarPorId(DomainId.de(id)))
    }

    @Test
    fun `buscar por identificador existente reconstitui o dominio`() {
        val doc = documento()
        org.mockito.Mockito.`when`(colecao.findById(doc.id)).thenReturn(Optional.of(doc))

        assertEquals("LUBRIFICANTES", adaptador.buscarPorId(DomainId.de(doc.id))?.codigo)
    }

    @Test
    fun `buscar por codigo normaliza caixa e espaco antes de consultar`() {
        org.mockito.Mockito.`when`(colecao.findByCodigo("LUBRIFICANTES")).thenReturn(documento())

        assertEquals("LUBRIFICANTES", adaptador.buscarPorCodigo("  lubrificantes  ")?.codigo)
    }

    @Test
    fun `existe com codigo tambem normaliza antes de consultar`() {
        org.mockito.Mockito.`when`(colecao.existsByCodigo("OLEOS")).thenReturn(true)

        assertTrue(adaptador.existeComCodigo(" oleos "))
        assertFalse(adaptador.existeComCodigo(" outros "), "codigo nao cadastrado nao pode responder que existe")
    }

    @Test
    fun `buscar por varios identificadores nao consulta o banco quando a lista e vazia`() {
        assertEquals(emptyMap(), adaptador.buscarPorIds(emptyList()))

        verify(colecao, never()).findAllById(org.mockito.ArgumentMatchers.anyIterable())
    }

    @Test
    fun `buscar por varios identificadores devolve mapa indexado pelo id`() {
        val primeira = documento(codigo = "LUBRIFICANTES")
        val segunda = documento(codigo = "FILTROS")
        val ids = listOf(primeira.id, segunda.id)
        org.mockito.Mockito.`when`(colecao.findAllById(ids)).thenReturn(listOf(primeira, segunda))

        val mapa = adaptador.buscarPorIds(ids.map(DomainId::de))

        assertEquals(2, mapa.size)
        assertEquals("LUBRIFICANTES", mapa[DomainId.de(primeira.id)]?.codigo)
        assertEquals("FILTROS", mapa[DomainId.de(segunda.id)]?.codigo)
    }

    @Test
    fun `listar sem termo usa findAll e esconde as desativadas por padrao`() {
        org.mockito.Mockito.`when`(colecao.findAll()).thenReturn(
            listOf(documento(codigo = "ATIVA"), documento(codigo = "MORTA", ativa = false))
        )

        assertEquals(listOf("ATIVA"), adaptador.listar(termo = null, incluirDesativadas = false).map { it.codigo })
    }

    @Test
    fun `listar com incluirDesativadas traz tambem as desativadas`() {
        org.mockito.Mockito.`when`(colecao.findAll()).thenReturn(
            listOf(documento(codigo = "ATIVA"), documento(codigo = "MORTA", ativa = false))
        )

        assertEquals(
            setOf("ATIVA", "MORTA"),
            adaptador.listar(termo = null, incluirDesativadas = true).map { it.codigo }.toSet(),
        )
    }

    @Test
    fun `listar com termo consulta por codigo ou nome, e nao varre a colecao`() {
        org.mockito.Mockito
            .`when`(colecao.findByCodigoContainingIgnoreCaseOrNomeContainingIgnoreCase("lubri", "lubri"))
            .thenReturn(listOf(documento(codigo = "LUBRIFICANTES")))

        val achadas = adaptador.listar(termo = "lubri", incluirDesativadas = false)

        assertEquals(listOf("LUBRIFICANTES"), achadas.map { it.codigo })
        verify(colecao, never()).findAll()
    }
}
