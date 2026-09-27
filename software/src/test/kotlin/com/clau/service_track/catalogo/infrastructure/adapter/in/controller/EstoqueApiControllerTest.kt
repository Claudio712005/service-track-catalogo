package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.EstoqueRepositoryMemoriaAdapter
import com.clau.service_track.catalogo.InsumoRepositoryMemoriaAdapter
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class EstoqueApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var insumos: InsumoRepositoryMemoriaAdapter

    @Autowired
    private lateinit var estoque: EstoqueRepositoryMemoriaAdapter

    @BeforeEach
    fun reiniciarAcervo() {
        insumos.reiniciar()
        estoque.reiniciar()
    }

    @Test
    fun `insumo sem movimento responde saldo zerado, nao 404`() {
        mockMvc.perform(get("/insumos/$OLEO/estoque"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sku").value("OL-5W30-SYN-1L"))
            .andExpect(jsonPath("$.unidadeDeMedida").value("LITRO"))
            .andExpect(jsonPath("$.quantidadeDisponivel").value(0))
            .andExpect(jsonPath("$.quantidadeReservada").value(0))
            .andExpect(jsonPath("$.abaixoDoMinimo").value(false))
            .andExpect(jsonPath("$.reservas", hasSize<Any>(0)))
    }

    @Test
    fun `insumo inexistente devolve 404`() {
        mockMvc.perform(get("/insumos/0f6f6a9c-1f1e-4a6a-9f8e-2b7c5d4e3a21/estoque"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
    }

    @Test
    fun `identificador fora do formato uuid devolve 400`() {
        mockMvc.perform(get("/insumos/nao-e-uuid/estoque"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("não é um UUID válido")))
    }

    @Test
    fun `entrada soma ao disponivel e devolve o saldo resultante`() {
        registrar(OLEO, "nf-4471-item-1", """{"quantidade":24,"custoUnitario":38.90}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantidadeDisponivel").value(24.0))

        mockMvc.perform(get("/insumos/$OLEO/estoque"))
            .andExpect(jsonPath("$.quantidadeDisponivel").value(24.0))
    }

    @Test
    fun `mesma chave de idempotencia nao soma de novo`() {
        registrar(OLEO, "nf-4471-item-1", """{"quantidade":24}""").andExpect(status().isOk)
        registrar(OLEO, "nf-4471-item-1", """{"quantidade":24}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantidadeDisponivel").value(24.0))
    }

    @Test
    fun `chave nova soma de novo, por ser entrada nova`() {
        registrar(OLEO, "nf-4471-item-1", """{"quantidade":24}""").andExpect(status().isOk)
        registrar(OLEO, "nf-4488-item-1", """{"quantidade":10}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantidadeDisponivel").value(34.0))
    }

    @Test
    fun `unidade inteira recusa entrada fracionaria`() {
        registrar(PNEU, "nf-4490-item-2", """{"quantidade":2.5}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REGRA_DE_NEGOCIO"))
            .andExpect(jsonPath("$.message", containsString("não admite fração")))
    }

    @Test
    fun `quantidade zero e recusada na borda`() {
        registrar(OLEO, "nf-4491-item-1", """{"quantidade":0}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
    }

    @Test
    fun `origem de ordem de servico nao se registra por http`() {
        registrar(OLEO, "nf-4492-item-1", """{"quantidade":5,"origemTipo":"ORDEM_SERVICO"}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("nasce de comando na fila")))
    }

    @Test
    fun `cabecalho de idempotencia ausente e recusado`() {
        mockMvc.perform(
            post("/insumos/$OLEO/estoque/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"quantidade":5}""")
        ).andExpect(status().isBadRequest)
    }

    private fun registrar(insumoId: String, chave: String, corpo: String) = mockMvc.perform(
        post("/insumos/$insumoId/estoque/entradas")
            .header("X-Idempotency-Key", chave)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo)
    )

    private companion object {
        const val OLEO = InsumoRepositoryMemoriaAdapter.OLEO_5W30
        const val PNEU = InsumoRepositoryMemoriaAdapter.PNEU_ARO16
    }
}
