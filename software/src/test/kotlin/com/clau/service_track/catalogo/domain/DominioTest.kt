package com.clau.service_track.catalogo.domain

import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DefinicaoDeAtributo
import com.clau.service_track.catalogo.domain.vo.Especificacao
import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorDeAtributo
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DominioTest {

    private val combustivel = CategoriaDeInsumo.criar(
        codigo = "combustivel",
        nome = "Combustível",
        unidadePadrao = UnidadeDeMedida.LITRO,
        atributos = listOf(
            DefinicaoDeAtributo(
                chave = "tipo", rotulo = "Tipo", tipo = TipoDeAtributo.OPCAO, obrigatorio = true,
                opcoes = listOf("Comum", "Aditivada", "Premium", "Etanol", "Diesel S10", "Diesel S500"),
            ),
            DefinicaoDeAtributo("octanagem", "Octanagem", TipoDeAtributo.INTEIRO, unidade = "RON"),
            DefinicaoDeAtributo("aditivado", "Aditivado", TipoDeAtributo.BOOLEANO),
        ),
    )

    private val pneu = CategoriaDeInsumo.criar(
        codigo = "pneu",
        nome = "Pneu",
        unidadePadrao = UnidadeDeMedida.UNIDADE,
        atributos = listOf(
            DefinicaoDeAtributo("largura", "Largura", TipoDeAtributo.INTEIRO, unidade = "mm", obrigatorio = true),
            DefinicaoDeAtributo("perfil", "Perfil", TipoDeAtributo.INTEIRO, unidade = "%", obrigatorio = true),
            DefinicaoDeAtributo("aro", "Aro", TipoDeAtributo.INTEIRO, unidade = "pol", obrigatorio = true),
            DefinicaoDeAtributo("indice-carga", "Índice de carga", TipoDeAtributo.INTEIRO),
            DefinicaoDeAtributo("calibragem", "Calibragem recomendada", TipoDeAtributo.DECIMAL, unidade = "psi"),
            DefinicaoDeAtributo(
                chave = "indice-velocidade", rotulo = "Índice de velocidade",
                tipo = TipoDeAtributo.OPCAO, opcoes = listOf("T", "H", "V", "W", "Y"),
            ),
        ),
    )

    private fun gasolina() = Insumo.criar(
        categoria = combustivel,
        nome = "Gasolina Premium",
        descricao = "Combustível para abastecimento de veículos em teste de rodagem",
        custo = ValorMonetario.de("7.29"),
        especificacao = mapOf("tipo" to "Premium", "octanagem" to "98", "aditivado" to "sim"),
        qtdEstoqueInicial = BigDecimal("200.0"),
        estoqueMinimo = BigDecimal("50.0"),
    )

    private fun pneuAro16() = Insumo.criar(
        categoria = pneu,
        nome = "Pneu 205/55 R16",
        descricao = "Pneu radial para automóvel de passeio",
        custo = ValorMonetario.de("459.90"),
        especificacao = mapOf(
            "largura" to "205", "perfil" to "55", "aro" to "16",
            "indice-carga" to "91", "indice-velocidade" to "V", "calibragem" to "32.0",
        ),
        qtdEstoqueInicial = BigDecimal("8"),
        estoqueMinimo = BigDecimal("4"),
    )

    @Test
    fun `cada categoria impoe o proprio conjunto de atributos`() {
        assertEquals(ValorDeAtributo.Opcao("Premium"), gasolina().especificacao["tipo"])
        assertEquals(ValorDeAtributo.Inteiro(98), gasolina().especificacao["octanagem"])
        assertEquals(ValorDeAtributo.Inteiro(16), pneuAro16().especificacao["aro"])
        assertEquals(ValorDeAtributo.Decimal(BigDecimal("32.0")), pneuAro16().especificacao["calibragem"])
    }

    @Test
    fun `a especificacao se descreve com rotulo e unidade`() {
        assertEquals(
            "Largura: 205 mm, Perfil: 55 %, Aro: 16 pol, Índice de carga: 91, " +
                "Calibragem recomendada: 32.0 psi, Índice de velocidade: V",
            pneuAro16().especificacao.descrever(pneu),
        )
    }

    @Test
    fun `a descricao respeita a ordem declarada na categoria`() {
        val texto = gasolina().especificacao.descrever(combustivel)
        assertEquals("Tipo: Premium, Octanagem: 98 RON, Aditivado: true", texto)
    }

    @Test
    fun `atributo fora da categoria e recusado`() {
        val e = assertFailsWith<DomainException> {
            Insumo.criar(
                categoria = pneu, nome = "Pneu", descricao = "x", custo = ValorMonetario.de("1.00"),
                especificacao = mapOf("largura" to "205", "perfil" to "55", "aro" to "16", "viscosidade" to "5W30"),
            )
        }
        assertTrue(e.message!!.contains("viscosidade"))
    }

    @Test
    fun `atributo obrigatorio ausente e recusado`() {
        val e = assertFailsWith<DomainException> {
            Insumo.criar(
                categoria = pneu, nome = "Pneu", descricao = "x", custo = ValorMonetario.de("1.00"),
                especificacao = mapOf("largura" to "205"),
            )
        }
        assertTrue(e.message!!.contains("perfil"))
        assertTrue(e.message!!.contains("aro"))
    }

    @Test
    fun `opcao fora do dominio declarado e recusada`() {
        val e = assertFailsWith<DomainException> {
            Especificacao.de(combustivel, mapOf("tipo" to "Querosene"))
        }
        assertTrue(e.message!!.contains("Comum"))
    }

    @Test
    fun `tipo incompativel e recusado`() {
        assertFailsWith<DomainException> {
            Especificacao.de(pneu, mapOf("largura" to "duzentos", "perfil" to "55", "aro" to "16"))
        }
    }

    @Test
    fun `litro admite fracao e unidade nao`() {
        val g = gasolina()
        g.reservar(BigDecimal("4.2"))
        assertEquals(BigDecimal("195.8"), g.qtdEstoque)

        val e = assertFailsWith<DomainException> { pneuAro16().reservar(BigDecimal("2.5")) }
        assertTrue(e.message!!.contains("não admite fração"))
    }

    @Test
    fun `reserva e devolucao fecham o ciclo de compensacao`() {
        val g = gasolina()
        g.reservar(BigDecimal("30.5"))
        g.adicionarAoEstoque(BigDecimal("30.5"))
        assertEquals(0, BigDecimal("200.0").compareTo(g.qtdEstoque))
    }

    @Test
    fun `reserva acima do saldo falha sem alterar estoque`() {
        val p = pneuAro16()
        assertFailsWith<ConflitoDeEstadoException> { p.reservar(BigDecimal("20")) }
        assertEquals(0, BigDecimal("8").compareTo(p.qtdEstoque))
    }

    @Test
    fun `custo acompanha quantidade fracionaria`() {
        assertEquals(ValorMonetario.de("30.62"), gasolina().calcularCusto(BigDecimal("4.2")))
    }

    @Test
    fun `atributo novo nao pode nascer obrigatorio`() {
        val e = assertFailsWith<DomainException> {
            pneu.acrescentarAtributo(
                DefinicaoDeAtributo("marca", "Marca", TipoDeAtributo.TEXTO, obrigatorio = true)
            )
        }
        assertTrue(e.message!!.contains("já cadastrados"))
    }

    @Test
    fun `categoria recusa atributo OPCAO sem opcoes`() {
        assertFailsWith<DomainException> {
            DefinicaoDeAtributo("cor", "Cor", TipoDeAtributo.OPCAO)
        }
    }
}
