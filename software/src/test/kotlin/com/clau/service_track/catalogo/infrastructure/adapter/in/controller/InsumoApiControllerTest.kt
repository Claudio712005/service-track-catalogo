package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.CategoriaRepositoryMemoriaAdapter
import com.clau.service_track.catalogo.InsumoRepositoryMemoriaAdapter
import org.junit.jupiter.api.BeforeEach
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
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
class InsumoApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var insumos: InsumoRepositoryMemoriaAdapter

    @Autowired
    private lateinit var categorias: CategoriaRepositoryMemoriaAdapter

    @BeforeEach
    fun reiniciarAcervo() {
        insumos.reiniciar()
        categorias.reiniciar()
    }

    @Test
    fun `lista apenas os insumos ativos por padrao`() {
        mockMvc.perform(get(ROTA))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
            .andExpect(jsonPath("$[*].sku", hasItem("OL-5W30-SYN-1L")))
    }

    @Test
    fun `inclui desativados quando solicitado`() {
        mockMvc.perform(get("$ROTA?incluirInativos=true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(3)))
    }

    @Test
    fun `filtra por categoria`() {
        mockMvc.perform(get("$ROTA?categoriaId=${CategoriaRepositoryMemoriaAdapter.PNEU}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].sku").value("PN-205-55-R16"))
    }

    @Test
    fun `filtra por termo no sku nome ou marca`() {
        mockMvc.perform(get("$ROTA?termo=pirelli"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].marca").value("Pirelli"))
    }

    @Test
    fun `busca por identificador devolve a especificacao convertida`() {
        mockMvc.perform(get("$ROTA/${InsumoRepositoryMemoriaAdapter.OLEO_5W30}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sku").value("OL-5W30-SYN-1L"))
            .andExpect(jsonPath("$.unidadeFracionavel").value(true))
            .andExpect(jsonPath("$.especificacao.viscosidade").value("5W30"))
            .andExpect(jsonPath("$.especificacao.sintetico").value("true"))
    }

    @Test
    fun `busca por sku ignora caixa`() {
        mockMvc.perform(get("$ROTA/sku/ol-5w30-syn-1l"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(InsumoRepositoryMemoriaAdapter.OLEO_5W30))
    }

    @Test
    fun `identificador inexistente devolve 404 no formato padrao`() {
        mockMvc.perform(get("$ROTA/0f6f6a9c-1f1e-4a6a-9f8e-2b7c5d4e3a21"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
            .andExpect(jsonPath("$.path").value("$ROTA/0f6f6a9c-1f1e-4a6a-9f8e-2b7c5d4e3a21"))
    }

    @Test
    fun `identificador fora do formato UUID devolve 400`() {
        mockMvc.perform(get("$ROTA/nao-e-uuid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REGRA_DE_NEGOCIO"))
    }

    @Test
    fun `cria insumo e devolve 201 com Location`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "${CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR}",
                  "sku": "ol-10w40-semi-1l",
                  "nome": "Óleo 10W40 semissintético 1L",
                  "descricao": "Óleo lubrificante semissintético, embalagem de 1 litro.",
                  "custo": 29.90,
                  "especificacao": { "viscosidade": "10W40", "especificacao-api": "SN", "sintetico": "nao" },
                  "marca": "Mobil",
                  "controlaLote": true,
                  "validadeEmDias": 365
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.sku").value("OL-10W40-SEMI-1L"))
            .andExpect(jsonPath("$.unidadeDeMedida").value("LITRO"))
            .andExpect(jsonPath("$.especificacao.sintetico").value("false"))
            .andExpect(jsonPath("$.ativo").value(true))
    }

    @Test
    fun `sku repetido devolve 409`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "${CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR}",
                  "sku": "OL-5W30-SYN-1L",
                  "nome": "Outro óleo",
                  "descricao": "Mesmo SKU de um insumo existente.",
                  "custo": 10.00,
                  "especificacao": { "viscosidade": "5W30", "especificacao-api": "SN" }
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLITO_DE_ESTADO"))
    }

    @Test
    fun `codigo de barras repetido devolve 409`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "${CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR}",
                  "sku": "OL-0W20-SYN-1L",
                  "nome": "Óleo 0W20 sintético 1L",
                  "descricao": "Código de barras de outro insumo.",
                  "custo": 52.00,
                  "especificacao": { "viscosidade": "0W20", "especificacao-api": "SP" },
                  "codigoBarras": "7891234567890"
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `atributo fora da categoria devolve 400 explicando o que declarar`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "${CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR}",
                  "sku": "OL-TESTE-1",
                  "nome": "Óleo de teste",
                  "descricao": "Atributo que a categoria não declara.",
                  "custo": 10.00,
                  "especificacao": { "viscosidade": "5W30", "especificacao-api": "SN", "aro": "16" }
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REGRA_DE_NEGOCIO"))
            .andExpect(jsonPath("$.message", containsString("aro")))
    }

    @Test
    fun `atributo obrigatorio ausente devolve 400`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "${CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR}",
                  "sku": "OL-TESTE-2",
                  "nome": "Óleo de teste",
                  "descricao": "Falta a especificação API, que é obrigatória.",
                  "custo": 10.00,
                  "especificacao": { "viscosidade": "5W30" }
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("especificacao-api")))
    }

    @Test
    fun `valor fora das opcoes declaradas devolve 400 listando as aceitas`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "${CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR}",
                  "sku": "OL-TESTE-3",
                  "nome": "Óleo de teste",
                  "descricao": "Viscosidade que não está nas opções.",
                  "custo": 10.00,
                  "especificacao": { "viscosidade": "20W50", "especificacao-api": "SN" }
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("0W20")))
    }

    @Test
    fun `categoria inexistente devolve 404`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "0f6f6a9c-1f1e-4a6a-9f8e-2b7c5d4e3a21",
                  "sku": "OL-TESTE-4",
                  "nome": "Óleo de teste",
                  "descricao": "Categoria que não existe.",
                  "custo": 10.00
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `corpo invalido devolve 400 com violacoes por campo`() {
        mockMvc.perform(
            post(ROTA).contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "categoriaId": "${CategoriaRepositoryMemoriaAdapter.OLEO_MOTOR}",
                  "sku": "ol",
                  "nome": "",
                  "descricao": "SKU curto e nome vazio.",
                  "custo": -1.00,
                  "codigoBarras": "789"
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
            .andExpect(jsonPath("$.violacoes").isArray)
    }

    @Test
    fun `atualiza substituindo a especificacao por inteiro`() {
        mockMvc.perform(
            put("$ROTA/${InsumoRepositoryMemoriaAdapter.OLEO_5W30}").contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "descricao": "Óleo sintético para motores a gasolina, flex e GNV, 1 litro.",
                  "custo": 41.50,
                  "especificacao": { "viscosidade": "5W30", "especificacao-api": "SP" },
                  "marca": "Lubrax",
                  "controlaLote": true,
                  "validadeEmDias": 900
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.custo").value(41.50))
            .andExpect(jsonPath("$.especificacao['especificacao-api']").value("SP"))
            .andExpect(jsonPath("$.especificacao['sintetico']").doesNotExist())
            .andExpect(jsonPath("$.validadeEmDias").value(900))
    }

    @Test
    fun `atualizacao sem atributo obrigatorio devolve 400`() {
        mockMvc.perform(
            put("$ROTA/${InsumoRepositoryMemoriaAdapter.PNEU_ARO16}").contentType(MediaType.APPLICATION_JSON).content(
                """
                {
                  "descricao": "Pneu radial, sem informar o aro.",
                  "custo": 470.00,
                  "especificacao": {}
                }
                """.trimIndent()
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("aro")))
    }

    @Test
    fun `desativa e depois recusa nova desativacao`() {
        mockMvc.perform(delete("$ROTA/${InsumoRepositoryMemoriaAdapter.PNEU_ARO16}"))
            .andExpect(status().isNoContent)
        mockMvc.perform(delete("$ROTA/${InsumoRepositoryMemoriaAdapter.PNEU_ARO16}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLITO_DE_ESTADO"))
    }

    @Test
    fun `devolve os cabecalhos de correlacao e transacao`() {
        mockMvc.perform(get(ROTA).header("X-Correlation-Id", "fluxo-de-teste-123"))
            .andExpect(status().isOk)
            .andExpect(header().string("X-Correlation-Id", "fluxo-de-teste-123"))
            .andExpect(header().exists("X-Transaction-Id"))
    }

    @Test
    fun `correlacao ausente e gerada pelo servidor`() {
        mockMvc.perform(get(ROTA))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Correlation-Id"))
    }

    private companion object {
        const val ROTA = "/insumos"
    }
}
