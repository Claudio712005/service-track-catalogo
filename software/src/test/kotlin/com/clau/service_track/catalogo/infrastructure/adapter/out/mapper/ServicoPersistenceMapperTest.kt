package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServicoPersistenceMapperTest {

    private val mapper = ServicoPersistenceMapper()

    private val criacao = LocalDateTime.of(2026, 10, 11, 9, 30, 15)
    private val atualizacao = LocalDateTime.of(2026, 10, 11, 14, 5, 0)

    private fun servico(valor: ValorMonetario? = ValorMonetario.de("289.90")) = Servico.reconstituir(
        id = DomainId.de(UUID.randomUUID().toString()),
        nome = "Troca de oleo",
        descricao = "Troca de oleo e filtro",
        valorReferencia = valor,
        dataCriacao = criacao,
        dataAtualizacao = atualizacao,
    )

    @Test
    fun `ida e volta preserva todo campo do servico`() {
        val original = servico()

        val devolta = mapper.paraDominio(mapper.paraEntidade(original))

        assertEquals(original.id, devolta.id)
        assertEquals(original.nome, devolta.nome)
        assertEquals(original.descricao, devolta.descricao)
        assertEquals(original.valorReferencia, devolta.valorReferencia)
        assertEquals(original.dataCriacao, devolta.dataCriacao)
        assertEquals(original.dataAtualizacao, devolta.dataAtualizacao)
        assertEquals(original.ativo, devolta.ativo)
    }

    @Test
    fun `valor de referencia ausente continua ausente`() {
        val devolta = mapper.paraDominio(mapper.paraEntidade(servico(valor = null)))

        assertNull(devolta.valorReferencia)
    }

    @Test
    fun `a data vai para a entidade em UTC`() {
        val entidade = mapper.paraEntidade(servico())

        assertEquals(0, entidade.dataCriacao.offset.totalSeconds, "a entidade tem de gravar em UTC")
        assertEquals(criacao, entidade.dataCriacao.toLocalDateTime())
    }

    @Test
    fun `atualizar entidade mexe so no que o dominio deixa mudar`() {
        val original = servico()
        val entidade = mapper.paraEntidade(original)
        val idOriginal = entidade.id
        val criacaoOriginal = entidade.dataCriacao

        val alterado = Servico.reconstituir(
            id = original.id,
            nome = "Troca de oleo sintetico",
            descricao = "Com filtro premium",
            valorReferencia = ValorMonetario.de("399.00"),
            dataCriacao = criacao,
            dataAtualizacao = atualizacao.plusHours(2),
            ativo = false,
        )

        val resultado = mapper.atualizarEntidade(entidade, alterado)

        assertTrue(resultado === entidade, "atualizar tem de mexer na instancia gerenciada pelo JPA")
        assertEquals(idOriginal, resultado.id)
        assertEquals(criacaoOriginal, resultado.dataCriacao)
        assertEquals("Com filtro premium", resultado.descricaoServico)
        assertEquals(BigDecimal("399.00"), resultado.valorReferencia)
        assertEquals(false, resultado.ativo)
        assertEquals(atualizacao.plusHours(2), resultado.dataAtualizacao.toLocalDateTime())

        assertEquals(
            "Troca de oleo",
            resultado.nomeServico,
            "nome e val no dominio: servico nao se renomeia, e o mapper nao pode abrir essa porta",
        )
    }
}
