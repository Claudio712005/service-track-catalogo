package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class ServicoApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `lista apenas os servicos ativos por padrao`() {
        mockMvc.perform(get(ROTA))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(7))
            .andExpect(jsonPath("$[0].nome").value("Alinhamento e balanceamento"))
            .andExpect(jsonPath("$[*].nome").value(not(hasItem("Troca de embreagem"))))
    }

    @Test
    fun `inclui inativos quando solicitado`() {
        mockMvc.perform(get("$ROTA?incluirInativos=true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(8))
    }

    @Test
    fun `busca por identificador devolve o servico`() {
        mockMvc.perform(get("$ROTA/$TROCA_DE_OLEO"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(TROCA_DE_OLEO))
            .andExpect(jsonPath("$.nome").value("Troca de óleo e filtro"))
            .andExpect(jsonPath("$.valorReferencia").value(189.90))
            .andExpect(jsonPath("$.ativo").value(true))
    }

    @Test
    fun `busca por identificador inexistente devolve 404 no formato padrao`() {
        mockMvc.perform(get("$ROTA/0f6f6a9c-1f1e-4a6a-9f8e-2b7c5d4e3a21"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.path").exists())
    }

    @Test
    fun `identificador fora do formato UUID devolve 400`() {
        mockMvc.perform(get("$ROTA/nao-e-uuid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REGRA_DE_NEGOCIO"))
    }

    @Test
    fun `cria servico e devolve 201 com Location`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """{"nome":"Troca de amortecedores","descricao":"Substituição dos quatro amortecedores com teste de rodagem.","valorReferencia":"890.00"}"""
            )
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.nome").value("Troca de amortecedores"))
            .andExpect(jsonPath("$.ativo").value(true))
    }

    @Test
    fun `nome repetido devolve 409`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """{"nome":"Diagnóstico eletrônico","descricao":"Duplicado de propósito.","valorReferencia":"100.00"}"""
            )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLITO_DE_ESTADO"))
    }

    @Test
    fun `corpo invalido devolve 400 com violacoes por campo`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """{"nome":"","descricao":"x","valorReferencia":"-10.00"}"""
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
            .andExpect(jsonPath("$.violacoes.length()").value(2))
    }

    @Test
    fun `atualiza descricao e valor`() {
        mockMvc.perform(
            put("$ROTA/$ALINHAMENTO").contentType(MediaType.APPLICATION_JSON).content(
                """{"descricao":"Alinhamento, balanceamento e cambagem em equipamento a laser.","valorReferencia":"179.90"}"""
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valorReferencia").value(179.90))
            .andExpect(jsonPath("$.descricao").value(containsString("cambagem")))
    }

    @Test
    fun `desativa e depois recusa nova desativacao`() {
        mockMvc.perform(delete("$ROTA/$DIAGNOSTICO")).andExpect(status().isNoContent)
        mockMvc.perform(delete("$ROTA/$DIAGNOSTICO"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLITO_DE_ESTADO"))
    }

    private companion object {
        const val ROTA = "/servicos"
        const val TROCA_DE_OLEO = "bbfdb1a8-66e2-4292-a6cb-6a6d3fb080fd"
        const val ALINHAMENTO = "4b0a0a22-4dd7-4367-95d7-6a3583b4c0ea"
        const val DIAGNOSTICO = "5d00cdcd-2e96-45be-98b2-c3ba803cb15a"
    }
}
