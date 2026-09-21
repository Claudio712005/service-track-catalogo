package com.clau.service_track.catalogo.application.port.`in`.api

import com.clau.service_track.catalogo.application.port.`in`.api.dto.AtualizarServicoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarServicoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.ServicoResponse
import com.clau.service_track.catalogo.infrastructure.adapter.web.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
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

@RequestMapping(path = ["/servicos"], version = "1")
@Tag(
    name = "Serviços",
    description = "Catálogo de serviços oferecidos pela oficina. Um serviço é a mão de obra " +
        "cobrável de uma ordem de serviço, distinta dos insumos consumidos na execução. " +
        "Este recurso é a fonte canônica de nome e valor de referência para composição de orçamentos."
)
interface IServicoApiPort {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "listarServicos",
        summary = "Lista os serviços do catálogo",
        description = "Retorna a coleção completa, ordenada por nome. O catálogo é limitado por " +
            "natureza e não é paginado; se isso mudar, a paginação entra como versão nova do recurso. " +
            "Por padrão os serviços desativados são omitidos."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Coleção retornada. Catálogo vazio responde 200 com lista vazia, não 404.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = ArraySchema(schema = Schema(implementation = ServicoResponse::class))
            )
        ]
    )
    fun listarServicos(
        @Parameter(
            description = "Inclui também os serviços desativados. Use para conferência " +
                "e auditoria; não use para montar orçamento.",
            example = "false"
        )
        @RequestParam(name = "incluirInativos", defaultValue = "false")
        incluirInativos: Boolean,
    ): List<ServicoResponse>

    @GetMapping(path = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "buscarServicoPorId",
        summary = "Consulta um serviço por identificador",
        description = "Retorna o serviço independentemente de estar ativo. Ordens de serviço " +
            "antigas referenciam serviços já desativados, e essa consulta precisa continuar " +
            "respondendo para que o histórico permaneça legível."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Serviço encontrado.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ServicoResponse::class))]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum serviço com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun buscarServicoPorId(
        @Parameter(
            description = "Identificador do serviço.",
            example = "bbfdb1a8-66e2-4292-a6cb-6a6d3fb080fd",
            required = true
        )
        @PathVariable id: String,
    ): ServicoResponse

    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        operationId = "criarServico",
        summary = "Cadastra um serviço no catálogo",
        description = "Cria o serviço já ativo, com identificador gerado pelo servidor. " +
            "O nome é único entre os serviços ativos: repetir um nome existente responde 409. " +
            "Um nome liberado por desativação pode ser reutilizado."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Serviço cadastrado. O cabeçalho Location aponta para o recurso criado.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ServicoResponse::class))]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Corpo inválido. O campo errors detalha cada violação por atributo.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class),
                examples = [
                    ExampleObject(
                        name = "Valor de referência negativo",
                        value = """
                        {
                          "timestamp": "2026-09-21T15:04:22.817-03:00",
                          "status": 400,
                          "error": "Bad Request",
                          "code": "REQUISICAO_INVALIDA",
                          "message": "Corpo da requisição contém 1 campo(s) inválido(s)",
                          "path": "/servicos",
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
                          "violacoes": [
                            {
                              "campo": "valorReferencia",
                              "mensagem": "Valor de referência não pode ser negativo",
                              "valorRejeitado": "-10.00"
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
        responseCode = "409",
        description = "Já existe um serviço ativo com esse nome.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun criarServico(
        @Valid @RequestBody requisicao: CriarServicoRequest,
    ): ResponseEntity<ServicoResponse>

    @PutMapping(
        path = ["/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        operationId = "atualizarServico",
        summary = "Altera descrição e valor de referência",
        description = "Substitui integralmente os campos alteráveis. O nome não é alterável: " +
            "ele identifica o serviço em ordens já emitidas, e renomear reescreveria o histórico. " +
            "Orçamentos anteriores mantêm o valor vigente na data de emissão."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Serviço atualizado.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ServicoResponse::class))]
    )
    @ApiResponse(
        responseCode = "400",
        description = "Corpo inválido.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum serviço com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "409",
        description = "Serviço desativado. Reative antes de alterar.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun atualizarServico(
        @Parameter(description = "Identificador do serviço.", example = "bbfdb1a8-66e2-4292-a6cb-6a6d3fb080fd", required = true)
        @PathVariable id: String,
        @Valid @RequestBody requisicao: AtualizarServicoRequest,
    ): ServicoResponse

    @DeleteMapping(path = ["/{id}"])
    @Operation(
        operationId = "desativarServico",
        summary = "Desativa um serviço",
        description = "Desativação lógica. O registro permanece consultável por identificador e " +
            "continua referenciado por ordens de serviço antigas; apenas deixa de aparecer nas " +
            "listagens e de compor novos orçamentos. Não há remoção física, porque isso romperia " +
            "o histórico mantido pelo serviço de ordens."
    )
    @ApiResponse(responseCode = "204", description = "Serviço desativado.")
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum serviço com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    @ApiResponse(
        responseCode = "409",
        description = "Serviço já estava desativado. A operação não é idempotente por decisão: " +
            "repetir indica divergência de estado no chamador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun desativarServico(
        @Parameter(description = "Identificador do serviço.", example = "bbfdb1a8-66e2-4292-a6cb-6a6d3fb080fd", required = true)
        @PathVariable id: String,
    )
}
