package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.CategoriaRepositoryMemoriaAdapter
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class CategoriaApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var categorias: CategoriaRepositoryMemoriaAdapter

    @BeforeEach
    fun reiniciarAcervo() {
        categorias.reiniciar()
    }

    @Test
    fun `lista as categorias com os atributos declarados`() {
        mockMvc.perform(get(ROTA))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].codigo").value("OLEO_MOTOR"))
            .andExpect(jsonPath("$[0].unidadeFracionavel").value(true))
            .andExpect(jsonPath("$[0].atributos", hasSize<Any>(3)))
            .andExpect(jsonPath("$[0].atributos[0].opcoes", hasSize<Any>(4)))
    }

    @Test
    fun `filtra por termo no codigo ou no nome`() {
        mockMvc.perform(get("$ROTA?termo=pneu"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].codigo").value("PNEU"))
    }

    @Test
    fun `busca por identificador devolve unidade e atributos`() {
        mockMvc.perform(get("$ROTA/${CategoriaRepositoryMemoriaAdapter.PNEU}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unidadePadrao").value("UNIDADE"))
            .andExpect(jsonPath("$.unidadeFracionavel").value(false))
            .andExpect(jsonPath("$.atributos[0].chave").value("aro"))
            .andExpect(jsonPath("$.atributos[0].unidade").value("pol"))
    }

    @Test
    fun `identificador inexistente devolve 404`() {
        mockMvc.perform(get("$ROTA/0f6f6a9c-1f1e-4a6a-9f8e-2b7c5d4e3a21"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
    }

    @Test
    fun `cria categoria e devolve 201 com Location`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "codigo": "bateria",
                  "nome": "Bateria automotiva",
                  "unidadePadrao": "UNIDADE",
                  "atributos": [
                    { "chave": "amperagem", "rotulo": "Amperagem", "tipo": "INTEIRO", "unidade": "Ah", "obrigatorio": true },
                    { "chave": "polaridade", "rotulo": "Polaridade", "tipo": "OPCAO", "opcoes": ["ESQUERDA", "DIREITA"] }
                  ]
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.codigo").value("BATERIA"))
            .andExpect(jsonPath("$.atributos", hasSize<Any>(2)))
    }

    @Test
    fun `codigo repetido devolve 409`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "codigo": "OLEO_MOTOR",
                  "nome": "Óleo de motor duplicado",
                  "unidadePadrao": "LITRO"
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLITO_DE_ESTADO"))
    }

    @Test
    fun `atributo do tipo OPCAO sem opcoes devolve 400`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "codigo": "FILTRO",
                  "nome": "Filtro",
                  "unidadePadrao": "PECA",
                  "atributos": [ { "chave": "tipo", "rotulo": "Tipo", "tipo": "OPCAO" } ]
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("OPCAO")))
    }

    @Test
    fun `chave fora do formato devolve 400 com violacao por campo`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "codigo": "FILTRO",
                  "nome": "Filtro",
                  "unidadePadrao": "PECA",
                  "atributos": [ { "chave": "Tipo Do Filtro", "rotulo": "Tipo", "tipo": "TEXTO" } ]
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
    }

    @Test
    fun `unidade inexistente devolve 400 listando as aceitas`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "codigo": "FILTRO",
                  "nome": "Filtro",
                  "unidadePadrao": "CAIXA"
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("LITRO")))
    }

    @Test
    fun `acrescenta atributo opcional na categoria`() {
        mockMvc.perform(
            post("$ROTA/${CategoriaRepositoryMemoriaAdapter.PNEU}/atributos")
                .contentType(MediaType.APPLICATION_JSON).content(
                    """
                    { "chave": "indice-carga", "rotulo": "Índice de carga", "tipo": "INTEIRO" }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.atributos", hasSize<Any>(2)))
            .andExpect(jsonPath("$.atributos[1].chave").value("indice-carga"))
    }

    @Test
    fun `atributo novo nao pode nascer obrigatorio`() {
        mockMvc.perform(
            post("$ROTA/${CategoriaRepositoryMemoriaAdapter.PNEU}/atributos")
                .contentType(MediaType.APPLICATION_JSON).content(
                    """
                    { "chave": "indice-velocidade", "rotulo": "Índice de velocidade", "tipo": "TEXTO", "obrigatorio": true }
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("obrigatório")))
    }

    @Test
    fun `chave repetida na categoria devolve 400`() {
        mockMvc.perform(
            post("$ROTA/${CategoriaRepositoryMemoriaAdapter.PNEU}/atributos")
                .contentType(MediaType.APPLICATION_JSON).content(
                    """
                    { "chave": "aro", "rotulo": "Aro novamente", "tipo": "INTEIRO" }
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("aro")))
    }

    private companion object {
        const val ROTA = "/categorias"
    }
}
