package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoriaPersistenceMapperTest {

    private val mapper = CategoriaPersistenceMapper()

    private val atributos = listOf(
        DefinicaoDeAtributo(
            chave = "viscosidade",
            rotulo = "Viscosidade",
            tipo = TipoDeAtributo.OPCAO,
            obrigatorio = true,
            opcoes = listOf("5W30", "20W50"),
        ),
        DefinicaoDeAtributo(
            chave = "volume",
            rotulo = "Volume",
            tipo = TipoDeAtributo.DECIMAL,
            unidade = "L",
        ),
    )

    private fun categoria() = CategoriaDeInsumo.reconstituir(
        id = DomainId.de(UUID.randomUUID().toString()),
        codigo = "LUBRIFICANTES",
        nome = "Lubrificantes",
        unidadePadrao = UnidadeDeMedida.LITRO,
        atributos = atributos,
    )

    @Test
    fun `ida e volta preserva codigo, unidade e cada atributo`() {
        val original = categoria()

        val devolta = mapper.paraDominio(mapper.paraDocumento(original, criadoEm = null))

        assertEquals(original.id, devolta.id)
        assertEquals(original.codigo, devolta.codigo)
        assertEquals(original.nome, devolta.nome)
        assertEquals(original.unidadePadrao, devolta.unidadePadrao)
        assertEquals(original.ativa, devolta.ativa)
        assertEquals(original.atributos, devolta.atributos)
    }

    @Test
    fun `atributo de opcao mantem as opcoes aceitas`() {
        val devolta = mapper.paraDominio(mapper.paraDocumento(categoria(), criadoEm = null))
        val viscosidade = devolta.atributos.single { it.chave == "viscosidade" }

        assertEquals(listOf("5W30", "20W50"), viscosidade.opcoes)
        assertTrue(viscosidade.obrigatorio)
        assertNull(viscosidade.unidade)
    }

    @Test
    fun `a data de criacao informada e preservada, e a de atualizacao e do momento da gravacao`() {
        val criacao = Instant.parse("2026-03-01T12:00:00Z")
        val antes = Instant.now()

        val documento = mapper.paraDocumento(categoria(), criadoEm = criacao)

        assertEquals(criacao, documento.dataCriacao)
        assertTrue(
            !documento.dataAtualizacao.isBefore(antes),
            "dataAtualizacao tem de ser do momento da gravacao, nao a informada",
        )
    }

    @Test
    fun `sem data de criacao informada o documento nasce com a de agora`() {
        val antes = Instant.now()

        val documento = mapper.paraDocumento(categoria(), criadoEm = null)

        assertTrue(!documento.dataCriacao.isBefore(antes))
    }

    @Test
    fun `tipoDe aceita o nome do tipo em qualquer caixa`() {
        assertEquals(TipoDeAtributo.DECIMAL, mapper.tipoDe("decimal"))
        assertEquals(TipoDeAtributo.OPCAO, mapper.tipoDe("OPCAO"))
        assertEquals(TipoDeAtributo.BOOLEANO, mapper.tipoDe("BooLeano"))
    }

    @Test
    fun `tipoDe recusa o que nao e tipo de atributo`() {
        assertFailsWith<IllegalArgumentException> { mapper.tipoDe("monetario") }
    }
}
