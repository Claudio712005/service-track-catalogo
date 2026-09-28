package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.CategoriaRepositoryMemoriaAdapter
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class AtivacaoDeCategoriaTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var categorias: CategoriaRepositoryMemoriaAdapter

    @Autowired
    private lateinit var insumos: InsumoRepositoryMemoriaAdapter

    @BeforeEach
    fun reiniciarAcervo() {
        categorias.reiniciar()
        insumos.reiniciar()
    }

    @Test
    fun `categoria nasce ativa`() {
        mockMvc.perform(get("$CATEGORIAS/$OLEO_MOTOR"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ativa").value(true))
    }

    @Test
    fun `desativar tira a categoria da listagem padrao e a devolve com a flag`() {
        alternar(OLEO_MOTOR, false)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ativa").value(false))

        mockMvc.perform(get(CATEGORIAS))
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].codigo").value("PNEU"))

        mockMvc.perform(get("$CATEGORIAS?incluirDesativadas=true"))
            .andExpect(jsonPath("$", hasSize<Any>(2)))
    }

    @Test
    fun `insumo de categoria desativada sai da listagem e continua consultavel`() {
        mockMvc.perform(get(INSUMOS))
            .andExpect(jsonPath("$", hasSize<Any>(2)))

        alternar(OLEO_MOTOR, false).andExpect(status().isOk)

        mockMvc.perform(get(INSUMOS))
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].sku").value("PN-205-55-R16"))

        mockMvc.perform(get("$INSUMOS/${InsumoRepositoryMemoriaAdapter.OLEO_5W30}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sku").value("OL-5W30-SYN-1L"))
    }

    @Test
    fun `reativar devolve os insumos a listagem`() {
        alternar(OLEO_MOTOR, false).andExpect(status().isOk)
        alternar(OLEO_MOTOR, true)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ativa").value(true))

        mockMvc.perform(get(INSUMOS)).andExpect(jsonPath("$", hasSize<Any>(2)))
    }

    @Test
    fun `categoria desativada nao aceita insumo novo`() {
        alternar(OLEO_MOTOR, false).andExpect(status().isOk)

        mockMvc.perform(
            post(INSUMOS).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "$OLEO_MOTOR",
                  "sku": "OL-0W20-SYN-1L",
                  "nome": "Óleo 0W20 sintético 1L",
                  "descricao": "Óleo sintético de baixa viscosidade.",
                  "custo": 52.40,
                  "especificacao": { "viscosidade": "0W20", "especificacao-api": "SP" }
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message", containsString("desativada e não aceita insumo novo")))
    }

    @Test
    fun `categoria desativada nao recebe atributo novo`() {
        alternar(OLEO_MOTOR, false).andExpect(status().isOk)

        mockMvc.perform(
            post("$CATEGORIAS/$OLEO_MOTOR/atributos").contentType(MediaType.APPLICATION_JSON).content(
                """{ "chave": "aditivado", "rotulo": "Aditivado", "tipo": "BOOLEANO" }"""
            )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message", containsString("não pode receber atributo")))
    }

    @Test
    fun `cadastrar com codigo de categoria desativada aponta a reativacao`() {
        alternar(OLEO_MOTOR, false).andExpect(status().isOk)

        mockMvc.perform(
            post(CATEGORIAS).contentType(MediaType.APPLICATION_JSON).content(
                """
                { "codigo": "OLEO_MOTOR", "nome": "Óleo de motor", "unidadePadrao": "LITRO" }
                """.trimIndent()
            )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message", containsString("atualmente desativada")))
            .andExpect(jsonPath("$.message", containsString("$CATEGORIAS/$OLEO_MOTOR/ativacao")))
    }

    @Test
    fun `cadastrar com codigo de categoria ativa segue sendo conflito simples`() {
        mockMvc.perform(
            post(CATEGORIAS).contentType(MediaType.APPLICATION_JSON).content(
                """
                { "codigo": "OLEO_MOTOR", "nome": "Óleo de motor", "unidadePadrao": "LITRO" }
                """.trimIndent()
            )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message", containsString("Já existe uma categoria ativa")))
    }

    @Test
    fun `desativar duas vezes e conflito`() {
        alternar(OLEO_MOTOR, false).andExpect(status().isOk)
        alternar(OLEO_MOTOR, false)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message", containsString("já está desativada")))
    }

    @Test
    fun `reativar categoria ativa e conflito`() {
        alternar(OLEO_MOTOR, true)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message", containsString("já está ativa")))
    }

    @Test
    fun `ativacao de categoria inexistente devolve 404`() {
        alternar("0f6f6a9c-1f1e-4a6a-9f8e-2b7c5d4e3a21", false)
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
    }

    private fun alternar(id: String, ativa: Boolean) = mockMvc.perform(
        put("$CATEGORIAS/$id/ativacao")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"ativa":$ativa}""")
    )

    private companion object {
        const val CATEGORIAS = "/categorias"
        const val INSUMOS = "/insumos"
        const val OLEO_MOTOR = CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR
    }
}
