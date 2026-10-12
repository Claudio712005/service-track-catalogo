package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.Especificacao
import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorDeAtributo
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsumoPersistenceMapperTest {

    private val mapper = InsumoPersistenceMapper()

    private val agora = LocalDateTime.of(2026, 10, 11, 7, 45, 0)

    private val categoria = CategoriaDeInsumo.reconstituir(
        id = DomainId.de(UUID.randomUUID().toString()),
        codigo = "LUBRIFICANTES",
        nome = "Lubrificantes",
        unidadePadrao = UnidadeDeMedida.LITRO,
        atributos = listOf(
            DefinicaoDeAtributo("viscosidade", "Viscosidade", TipoDeAtributo.OPCAO, opcoes = listOf("5W30", "20W50")),
            DefinicaoDeAtributo("volume", "Volume", TipoDeAtributo.DECIMAL, unidade = "L"),
            DefinicaoDeAtributo("sintetico", "Sintetico", TipoDeAtributo.BOOLEANO),
        ),
    )

    private fun insumo(especificacao: Especificacao = Especificacao.de(
        categoria,
        mapOf("viscosidade" to "5W30", "volume" to "1.0", "sintetico" to "sim"),
    )) = Insumo.reconstituir(
        id = DomainId.de(UUID.randomUUID().toString()),
        categoriaId = categoria.id,
        sku = "OL-5W30-SIN-1L",
        nome = "Oleo 5W30 sintetico",
        descricao = "Oleo de motor",
        unidadeDeMedida = UnidadeDeMedida.LITRO,
        custo = ValorMonetario.de("38.50"),
        especificacao = especificacao,
        dataCriacao = agora,
        dataAtualizacao = agora,
        marca = "Marca X",
        fabricante = "Fabricante Y",
        codigoFabricante = "FY-123",
        codigoBarras = "7891234567890",
        controlaLote = true,
        validadeEmDias = 730,
    )

    @Test
    fun `ida e volta preserva identificacao, custo e os campos opcionais`() {
        val original = insumo()

        val devolta = mapper.paraDominio(mapper.paraDocumento(original), categoria)

        assertEquals(original.id, devolta.id)
        assertEquals(original.categoriaId, devolta.categoriaId)
        assertEquals(original.sku, devolta.sku)
        assertEquals(original.nome, devolta.nome)
        assertEquals(original.descricao, devolta.descricao)
        assertEquals(original.unidadeDeMedida, devolta.unidadeDeMedida)
        assertEquals(original.custo, devolta.custo)
        assertEquals(original.marca, devolta.marca)
        assertEquals(original.fabricante, devolta.fabricante)
        assertEquals(original.codigoFabricante, devolta.codigoFabricante)
        assertEquals(original.codigoBarras, devolta.codigoBarras)
        assertEquals(original.controlaLote, devolta.controlaLote)
        assertEquals(original.validadeEmDias, devolta.validadeEmDias)
        assertEquals(original.ativo, devolta.ativo)
        assertEquals(agora, devolta.dataCriacao)
        assertEquals(agora, devolta.dataAtualizacao)
    }

    @Test
    fun `com a categoria em maos a especificacao volta tipada`() {
        val devolta = mapper.paraDominio(mapper.paraDocumento(insumo()), categoria)

        assertEquals(ValorDeAtributo.Opcao("5W30"), devolta.especificacao["viscosidade"])
        assertEquals(ValorDeAtributo.Decimal(BigDecimal("1.0")), devolta.especificacao["volume"])
        assertEquals(ValorDeAtributo.Booleano(true), devolta.especificacao["sintetico"])
    }

    @Test
    fun `sem a categoria a especificacao volta como texto, e nao se perde`() {
        val devolta = mapper.paraDominio(mapper.paraDocumento(insumo()), categoria = null)

        assertEquals(ValorDeAtributo.Texto("5W30"), devolta.especificacao["viscosidade"])
        assertEquals(ValorDeAtributo.Texto("true"), devolta.especificacao["sintetico"])
        assertTrue(devolta.especificacao.contem("volume"))
    }

    @Test
    fun `valor que a categoria nao sabe converter sobrevive como texto`() {
        val documento = mapper.paraDocumento(insumo())
        documento.especificacao["volume"] = "um litro"

        val devolta = mapper.paraDominio(documento, categoria)

        assertEquals(ValorDeAtributo.Texto("um litro"), devolta.especificacao["volume"])
    }

    @Test
    fun `especificacao vazia volta vazia, sem consultar a categoria`() {
        val devolta = mapper.paraDominio(
            mapper.paraDocumento(insumo(especificacao = Especificacao.VAZIA)),
            categoria = null,
        )

        assertEquals(Especificacao.VAZIA, devolta.especificacao)
    }

    @Test
    fun `a data vai para o documento em UTC`() {
        val documento = mapper.paraDocumento(insumo())

        assertEquals(agora, LocalDateTime.ofInstant(documento.dataCriacao, java.time.ZoneOffset.UTC))
    }

    @Test
    fun `atualizar documento nao troca sku nem categoria`() {
        val original = insumo()
        val documento = mapper.paraDocumento(original)
        val skuOriginal = documento.sku
        val categoriaOriginal = documento.categoriaId
        val criacaoOriginal = documento.dataCriacao

        val alterado = Insumo.reconstituir(
            id = original.id,
            categoriaId = DomainId.de(UUID.randomUUID().toString()),
            sku = "OUTRO-SKU",
            nome = original.nome,
            descricao = "Descricao nova",
            unidadeDeMedida = UnidadeDeMedida.LITRO,
            custo = ValorMonetario.de("44.00"),
            especificacao = Especificacao.VAZIA,
            dataCriacao = agora,
            dataAtualizacao = agora.plusDays(1),
            marca = null,
            controlaLote = false,
            validadeEmDias = null,
            ativo = false,
        )

        val resultado = mapper.atualizarDocumento(documento, alterado)

        assertTrue(resultado === documento)
        assertEquals(skuOriginal, resultado.sku, "SKU e identidade de negocio: atualizar nao o troca")
        assertEquals(categoriaOriginal, resultado.categoriaId, "insumo nao troca de categoria por atualizacao")
        assertEquals(criacaoOriginal, resultado.dataCriacao)
        assertEquals("Descricao nova", resultado.descricao)
        assertEquals(BigDecimal("44.00"), resultado.custoPadrao)
        assertEquals(false, resultado.ativo)
        assertEquals(false, resultado.controlaLote)
        assertNull(resultado.validadeEmDias)
        assertNull(resultado.marca)
        assertTrue(resultado.especificacao.isEmpty())
    }
}
