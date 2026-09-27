package com.clau.service_track.catalogo.application.port.`in`.api

import com.clau.service_track.catalogo.application.port.`in`.api.dto.AtivacaoDeCategoriaRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CategoriaResponse
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarCategoriaRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.DefinicaoDeAtributoRequest
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping(path = ["/categorias"], version = "1")
@Tag(
    name = "Categorias de insumo",
    description = "Categorias agrupam insumos que compartilham o mesmo conjunto de características. " +
        "É aqui que se declara que óleo tem viscosidade e que pneu tem índice de carga: o operador " +
        "cadastra o atributo pela API, sem alteração de código nem migração de banco. " +
        "Cadastrar categoria é pré-requisito para cadastrar insumo."
)
interface ICategoriaApiPort {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "listarCategorias",
        summary = "Lista as categorias de insumo",
        description = "Retorna as categorias ativas, ordenadas por nome, cada uma com os atributos " +
            "que declara. O conjunto é pequeno por natureza e não é paginado. Categorias " +
            "desativadas só aparecem com incluirDesativadas=true."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Coleção retornada. Sem categorias cadastradas, responde 200 com lista vazia.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = ArraySchema(schema = Schema(implementation = CategoriaResponse::class)),
                examples = [
                    ExampleObject(
                        name = "Duas categorias",
                        value = """
                        [
                          {
                            "id": "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
                            "codigo": "OLEO_MOTOR",
                            "nome": "Óleo de motor",
                            "unidadePadrao": "LITRO",
                            "unidadeFracionavel": true,
                            "atributos": [
                              {
                                "chave": "viscosidade",
                                "rotulo": "Viscosidade",
                                "tipo": "OPCAO",
                                "unidade": null,
                                "obrigatorio": true,
                                "opcoes": ["0W20", "5W30", "10W40", "15W40"]
                              },
                              {
                                "chave": "especificacao-api",
                                "rotulo": "Especificação API",
                                "tipo": "TEXTO",
                                "unidade": null,
                                "obrigatorio": true,
                                "opcoes": []
                              }
                            ]
                          },
                          {
                            "id": "018f2ca1-2b77-7f10-8c02-91ab7d4e5f20",
                            "codigo": "PNEU",
                            "nome": "Pneu",
                            "unidadePadrao": "UNIDADE",
                            "unidadeFracionavel": false,
                            "atributos": [
                              {
                                "chave": "aro",
                                "rotulo": "Aro",
                                "tipo": "INTEIRO",
                                "unidade": "pol",
                                "obrigatorio": true,
                                "opcoes": []
                              }
                            ]
                          }
                        ]
                        """
                    )
                ]
            )
        ]
    )
    fun listarCategorias(
        @Parameter(
            description = "Filtra por parte do código ou do nome, ignorando caixa. " +
                "Omita para receber todas.",
            example = "oleo"
        )
        @RequestParam(name = "termo", required = false)
        termo: String?,
        @Parameter(
            description = "Inclui na resposta as categorias desativadas. Padrão false.",
            example = "false"
        )
        @RequestParam(name = "incluirDesativadas", required = false, defaultValue = "false")
        incluirDesativadas: Boolean,
    ): List<CategoriaResponse>

    @GetMapping(path = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "buscarCategoriaPorId",
        summary = "Consulta uma categoria por identificador",
        description = "Use esta consulta antes de cadastrar insumo: a resposta diz exatamente quais " +
            "chaves o mapa especificacao aceita, quais são obrigatórias e quais valores cada " +
            "atributo do tipo OPCAO admite."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Categoria encontrada.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = CategoriaResponse::class))]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Identificador fora do formato UUID.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhuma categoria com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun buscarCategoriaPorId(
        @Parameter(
            description = "Identificador da categoria.",
            example = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
            required = true
        )
        @PathVariable id: String,
    ): CategoriaResponse

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "criarCategoria",
        summary = "Cria uma categoria de insumo",
        description = "Cria a categoria com o conjunto inicial de atributos. O código é normalizado " +
            "para maiúsculas e é único. Os atributos podem nascer obrigatórios aqui, porque ainda " +
            "não existe insumo na categoria; depois de existir, atributo novo nasce opcional."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Categoria criada. O cabeçalho Location aponta para o recurso.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CategoriaResponse::class),
                examples = [
                    ExampleObject(
                        name = "Óleo de motor",
                        value = """
                        {
                          "id": "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
                          "codigo": "OLEO_MOTOR",
                          "nome": "Óleo de motor",
                          "unidadePadrao": "LITRO",
                          "unidadeFracionavel": true,
                          "atributos": [
                            {
                              "chave": "viscosidade",
                              "rotulo": "Viscosidade",
                              "tipo": "OPCAO",
                              "unidade": null,
                              "obrigatorio": true,
                              "opcoes": ["0W20", "5W30", "10W40", "15W40"]
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
        responseCode = "400",
        description = "Corpo inválido, ou atributo mal declarado — tipo OPCAO sem opções, " +
            "opções em tipo que não é OPCAO, chave fora do formato, ou chave repetida na categoria.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class),
                examples = [
                    ExampleObject(
                        name = "OPCAO sem opções",
                        value = """
                        {
                          "timestamp": "2026-09-26T10:14:02.481-03:00",
                          "status": 400,
                          "error": "Bad Request",
                          "code": "REGRA_DE_NEGOCIO",
                          "message": "Atributo 'viscosidade' é do tipo OPCAO e precisa declarar as opções aceitas",
                          "path": "/categorias",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                        }
                        """
                    )
                ]
            )
        ]
    )
    @ApiResponse(
        responseCode = "409",
        description = "Já existe categoria com esse código.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun criarCategoria(
        @Valid @RequestBody requisicao: CriarCategoriaRequest,
    ): ResponseEntity<CategoriaResponse>

    @PostMapping(
        path = ["/{id}/atributos"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        operationId = "acrescentarAtributoNaCategoria",
        summary = "Acrescenta um atributo à categoria",
        description = "Declara uma característica nova para os insumos da categoria, em tempo de " +
            "execução. O atributo **não pode nascer obrigatório**: os insumos já cadastrados não o " +
            "possuem, e exigi-lo tornaria inválido o acervo existente. Para torná-lo obrigatório " +
            "depois, preencha o atributo em todos os insumos e promova a exigência numa alteração " +
            "posterior da categoria."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Atributo acrescentado. A resposta traz a categoria completa.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = CategoriaResponse::class))]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Atributo mal declarado, ou marcado como obrigatório.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class),
                examples = [
                    ExampleObject(
                        name = "Obrigatório na inclusão",
                        value = """
                        {
                          "timestamp": "2026-09-26T10:22:31.109-03:00",
                          "status": 400,
                          "error": "Bad Request",
                          "code": "REGRA_DE_NEGOCIO",
                          "message": "Atributo 'aditivado' não pode nascer obrigatório: os insumos já cadastrados não o possuem. Cadastre como opcional, preencha o acervo e então torne obrigatório",
                          "path": "/categorias/018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11/atributos",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                        }
                        """
                    )
                ]
            )
        ]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhuma categoria com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "409",
        description = "A categoria já possui atributo com essa chave.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun acrescentarAtributo(
        @Parameter(description = "Identificador da categoria.", example = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11", required = true)
        @PathVariable id: String,
        @Valid @RequestBody requisicao: DefinicaoDeAtributoRequest,
    ): CategoriaResponse

    @PutMapping(
        path = ["/{id}/ativacao"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        operationId = "alternarAtivacaoDaCategoria",
        summary = "Desativa ou reativa a categoria",
        description = "Categoria não é apagada, é desligada. Enquanto desativada: não aceita " +
            "insumo novo, não recebe atributo novo, sai da listagem de categorias e **os seus " +
            "insumos saem da listagem padrão de insumos**. Nada é perdido — insumo da categoria " +
            "desativada continua consultável por identificador e por SKU, e volta à listagem " +
            "quando a categoria é reativada. Cadastrar uma categoria com código já usado por " +
            "uma categoria desativada é recusado com 409 apontando este endpoint."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Estado alterado. A resposta traz a categoria completa com o novo estado.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CategoriaResponse::class),
                examples = [
                    ExampleObject(
                        name = "Categoria desativada",
                        value = """
                        {
                          "id": "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
                          "codigo": "OLEO_MOTOR",
                          "nome": "Óleo de motor",
                          "unidadePadrao": "LITRO",
                          "unidadeFracionavel": true,
                          "ativa": false,
                          "atributos": []
                        }
                        """
                    )
                ]
            )
        ]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhuma categoria com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "409",
        description = "A categoria já está no estado pedido.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class),
                examples = [
                    ExampleObject(
                        name = "Já desativada",
                        value = """
                        {
                          "timestamp": "2026-09-27T09:41:18.204-03:00",
                          "status": 409,
                          "error": "Conflict",
                          "code": "CONFLITO_DE_ESTADO",
                          "message": "Categoria 'Óleo de motor' já está desativada",
                          "path": "/categorias/018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11/ativacao",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                        }
                        """
                    )
                ]
            )
        ]
    )
    fun alternarAtivacao(
        @Parameter(description = "Identificador da categoria.", example = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11", required = true)
        @PathVariable id: String,
        @Valid @RequestBody requisicao: AtivacaoDeCategoriaRequest,
    ): CategoriaResponse
}
