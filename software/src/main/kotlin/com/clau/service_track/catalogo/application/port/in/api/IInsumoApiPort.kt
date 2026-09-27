package com.clau.service_track.catalogo.application.port.`in`.api

import com.clau.service_track.catalogo.application.port.`in`.api.dto.AtualizarInsumoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarInsumoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.InsumoResponse
import com.clau.service_track.catalogo.infrastructure.adapter.web.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping(path = ["/insumos"], version = "1")
@Tag(
    name = "Insumos",
    description = "Cadastro dos materiais consumidos na execução de serviços: óleo, filtro, pastilha, " +
        "pneu. Este recurso responde o que o material **é** — identificação, custo de tabela e as " +
        "características específicas da categoria dele. **Quanto existe em estoque não vem daqui**: " +
        "saldo, reserva e movimentação são de outro recurso, ainda não publicado. " +
        "O insumo referenciado por uma ordem de serviço nunca é removido, apenas desativado."
)
interface IInsumoApiPort {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "listarInsumos",
        summary = "Lista insumos do catálogo",
        description = "Retorna os insumos ordenados por nome, com os filtros combináveis abaixo. " +
            "Por padrão os desativados são omitidos. O catálogo de uma oficina é da ordem de " +
            "milhares de itens e não é paginado nesta versão; paginação, se necessária, entra como " +
            "versão nova do recurso."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Coleção retornada. Filtro sem correspondência responde 200 com lista vazia, não 404.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = ArraySchema(schema = Schema(implementation = InsumoResponse::class)),
                examples = [
                    ExampleObject(
                        name = "Um insumo de óleo",
                        value = """
                        [
                          {
                            "id": "018f30bb-77a1-7c22-9b10-2a44de81f0aa",
                            "categoriaId": "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
                            "sku": "OL-5W30-SYN-1L",
                            "nome": "Óleo 5W30 sintético 1L",
                            "descricao": "Óleo lubrificante sintético para motores a gasolina e flex, embalagem de 1 litro.",
                            "unidadeDeMedida": "LITRO",
                            "unidadeFracionavel": true,
                            "custo": 38.90,
                            "especificacao": {
                              "viscosidade": "5W30",
                              "especificacao-api": "SN",
                              "sintetico": "true"
                            },
                            "marca": "Lubrax",
                            "fabricante": "Petrobras",
                            "codigoFabricante": "LB-5W30-1L",
                            "codigoBarras": "7891234567890",
                            "controlaLote": true,
                            "validadeEmDias": 730,
                            "ativo": true,
                            "dataCriacao": "2026-09-26T10:15:00",
                            "dataAtualizacao": "2026-09-26T10:15:00"
                          }
                        ]
                        """
                    )
                ]
            )
        ]
    )
    fun listarInsumos(
        @Parameter(
            description = "Inclui também os insumos desativados. Use para conferência e auditoria; " +
                "não use para montar orçamento.",
            example = "false"
        )
        @RequestParam(name = "incluirInativos", defaultValue = "false")
        incluirInativos: Boolean,

        @Parameter(
            description = "Restringe a uma categoria. Informe o identificador devolvido por GET /categorias.",
            example = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11"
        )
        @RequestParam(name = "categoriaId", required = false)
        categoriaId: String?,

        @Parameter(
            description = "Busca por parte do SKU, do nome ou da marca, ignorando caixa e acento " +
                "de digitação parcial. Use o que o operador digita no balcão.",
            example = "5W30"
        )
        @RequestParam(name = "termo", required = false)
        termo: String?,
    ): List<InsumoResponse>

    @GetMapping(path = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "buscarInsumoPorId",
        summary = "Consulta um insumo por identificador",
        description = "Retorna o insumo independentemente de estar ativo. Ordens de serviço antigas " +
            "referenciam insumos já desativados, e essa consulta precisa continuar respondendo para " +
            "que o histórico permaneça legível."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Insumo encontrado.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = InsumoResponse::class))]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Identificador fora do formato UUID.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum insumo com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun buscarInsumoPorId(
        @Parameter(
            description = "Identificador do insumo.",
            example = "018f30bb-77a1-7c22-9b10-2a44de81f0aa",
            required = true
        )
        @PathVariable id: String,
    ): InsumoResponse

    @GetMapping(path = ["/sku/{sku}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "buscarInsumoPorSku",
        summary = "Consulta um insumo por SKU",
        description = "Atalho para o balcão e para integração com leitor de código: o SKU é único e " +
            "estável, e é o que está impresso na etiqueta de prateleira. A busca ignora caixa."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Insumo encontrado.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = InsumoResponse::class))]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum insumo com esse SKU.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun buscarInsumoPorSku(
        @Parameter(description = "SKU do insumo.", example = "OL-5W30-SYN-1L", required = true)
        @PathVariable sku: String,
    ): InsumoResponse

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "criarInsumo",
        summary = "Cadastra um insumo no catálogo",
        description = "Cria o insumo já ativo, com identificador gerado pelo servidor e **sem saldo " +
            "em estoque**. O mapa especificacao é validado contra a categoria informada: só aceita " +
            "as chaves que ela declara, exige as obrigatórias e converte cada valor para o tipo " +
            "declarado. SKU e código de barras são únicos no catálogo."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Insumo cadastrado. O cabeçalho Location aponta para o recurso criado.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = InsumoResponse::class))]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Corpo inválido, ou especificação incompatível com a categoria — chave não " +
            "declarada, obrigatório ausente, valor fora das opções, ou tipo incompatível.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class),
                examples = [
                    ExampleObject(
                        name = "Atributo fora da categoria",
                        value = """
                        {
                          "timestamp": "2026-09-26T10:31:08.402-03:00",
                          "status": 400,
                          "error": "Bad Request",
                          "code": "REGRA_DE_NEGOCIO",
                          "message": "Atributo(s) não previsto(s) na categoria 'Óleo de motor': aro. Declare na categoria antes de usar",
                          "path": "/insumos",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                        }
                        """
                    ),
                    ExampleObject(
                        name = "Valor fora das opções declaradas",
                        value = """
                        {
                          "timestamp": "2026-09-26T10:33:57.771-03:00",
                          "status": 400,
                          "error": "Bad Request",
                          "code": "REGRA_DE_NEGOCIO",
                          "message": "Valor '20W50' não é aceito em 'Viscosidade'. Opções: 0W20, 5W30, 10W40, 15W40",
                          "path": "/insumos",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                        }
                        """
                    ),
                    ExampleObject(
                        name = "Campo do corpo inválido",
                        value = """
                        {
                          "timestamp": "2026-09-26T10:35:12.004-03:00",
                          "status": 400,
                          "error": "Bad Request",
                          "code": "REQUISICAO_INVALIDA",
                          "message": "Corpo da requisição contém 1 campo(s) inválido(s)",
                          "path": "/insumos",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
                          "violacoes": [
                            {
                              "campo": "codigoBarras",
                              "mensagem": "Código de barras admite de 8 a 14 dígitos",
                              "valorRejeitado": "789"
                            }
                          ]
                        }
                        """
                    )
                ]
            )
        ]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Categoria informada não existe.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "409",
        description = "SKU ou código de barras já cadastrado em outro insumo.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class),
                examples = [
                    ExampleObject(
                        name = "SKU repetido",
                        value = """
                        {
                          "timestamp": "2026-09-26T10:37:44.918-03:00",
                          "status": 409,
                          "error": "Conflict",
                          "code": "CONFLITO_DE_ESTADO",
                          "message": "Já existe um insumo cadastrado com o SKU 'OL-5W30-SYN-1L'",
                          "path": "/insumos",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                        }
                        """
                    )
                ]
            )
        ]
    )
    fun criarInsumo(
        @Valid @RequestBody requisicao: CriarInsumoRequest,
    ): ResponseEntity<InsumoResponse>

    @PutMapping(
        path = ["/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        operationId = "atualizarInsumo",
        summary = "Altera os dados de um insumo",
        description = "Substitui integralmente os campos alteráveis, **inclusive a especificação**: " +
            "atributo que não vier no corpo é removido do insumo. SKU, nome, categoria e unidade de " +
            "medida não são alteráveis. Insumo desativado recusa alteração com 409."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Insumo atualizado.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = InsumoResponse::class))]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Corpo inválido, ou especificação incompatível com a categoria do insumo.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum insumo com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "409",
        description = "Insumo desativado, ou código de barras pertencente a outro insumo.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun atualizarInsumo(
        @Parameter(description = "Identificador do insumo.", example = "018f30bb-77a1-7c22-9b10-2a44de81f0aa", required = true)
        @PathVariable id: String,
        @Valid @RequestBody requisicao: AtualizarInsumoRequest,
    ): InsumoResponse

    @DeleteMapping(path = ["/{id}"])
    @Operation(
        operationId = "desativarInsumo",
        summary = "Desativa um insumo",
        description = "Desativação lógica. O registro permanece consultável por identificador e por " +
            "SKU, e continua referenciado por ordens de serviço antigas; apenas deixa de aparecer " +
            "nas listagens e de entrar em orçamento novo. Não há remoção física, porque isso romperia " +
            "o histórico mantido pelo serviço de ordens e o razão de estoque."
    )
    @ApiResponse(responseCode = "204", description = "Insumo desativado.")
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum insumo com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "409",
        description = "Insumo já estava desativado. A operação não é idempotente por decisão: " +
            "repetir indica divergência de estado no chamador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun desativarInsumo(
        @Parameter(description = "Identificador do insumo.", example = "018f30bb-77a1-7c22-9b10-2a44de81f0aa", required = true)
        @PathVariable id: String,
    )
}
