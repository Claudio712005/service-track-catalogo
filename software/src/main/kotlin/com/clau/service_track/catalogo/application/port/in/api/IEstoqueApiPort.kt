package com.clau.service_track.catalogo.application.port.`in`.api

import com.clau.service_track.catalogo.application.port.`in`.api.dto.EntradaDeEstoqueRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.SaldoDeInsumoResponse
import com.clau.service_track.catalogo.infrastructure.adapter.web.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(path = ["/insumos/{insumoId}/estoque"], version = "1")
@Tag(
    name = "Estoque de insumo",
    description = "Saldo por insumo, com disponível e reservado separados. " +
        "Reserva, consumo e liberação **não têm endpoint**: chegam como comando no tópico " +
        "`servicetrack.estoque.comandos.v1` e são passos da saga da ordem de serviço. " +
        "Por HTTP ficam a consulta do saldo e a entrada de estoque, que é ação de operador."
)
interface IEstoqueApiPort {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        operationId = "consultarSaldoDoInsumo",
        summary = "Consulta o saldo de um insumo",
        description = "Insumo que nunca recebeu movimento responde 200 com saldo zerado, não 404: " +
            "o insumo existe, o saldo é que ainda não. O 404 fica para insumo inexistente."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Saldo do insumo, com as reservas ativas.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = SaldoDeInsumoResponse::class),
                examples = [
                    ExampleObject(
                        name = "Com reserva ativa",
                        value = """
                        {
                          "insumoId": "018f3c10-9a12-7b44-8e01-2c5d7f8a9b31",
                          "sku": "OL-5W30-SN-1L",
                          "nome": "Óleo 5W30 sintético 1L",
                          "unidadeDeMedida": "LITRO",
                          "quantidadeDisponivel": 18.0000,
                          "quantidadeReservada": 6.0000,
                          "estoqueMinimo": 10.0000,
                          "abaixoDoMinimo": false,
                          "dataAtualizacao": "2026-09-27T12:41:07.882",
                          "reservas": [
                            {
                              "reservaId": "018f4a02-31bc-7d55-9f21-7a0c4e2b6d10",
                              "ordemServicoId": "018f4a01-1120-7e33-8a10-5c9b3d7f2e40",
                              "quantidade": 6.0000,
                              "expiraEm": "2026-09-27T18:00:00"
                            }
                          ]
                        }
                        """
                    ),
                    ExampleObject(
                        name = "Insumo sem movimento",
                        value = """
                        {
                          "insumoId": "018f3c10-9a12-7b44-8e01-2c5d7f8a9b31",
                          "sku": "PN-195-55-R15",
                          "nome": "Pneu 195/55 R15",
                          "unidadeDeMedida": "UNIDADE",
                          "quantidadeDisponivel": 0.0000,
                          "quantidadeReservada": 0.0000,
                          "estoqueMinimo": 0.0000,
                          "abaixoDoMinimo": false,
                          "dataAtualizacao": "2026-09-27T12:55:30.117",
                          "reservas": []
                        }
                        """
                    )
                ]
            )
        ]
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
    fun consultarSaldo(
        @Parameter(
            description = "Identificador do insumo.",
            example = "018f3c10-9a12-7b44-8e01-2c5d7f8a9b31",
            required = true
        )
        @PathVariable insumoId: String,
    ): SaldoDeInsumoResponse

    @PostMapping(
        path = ["/entradas"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        operationId = "registrarEntradaDeEstoque",
        summary = "Registra entrada de estoque",
        description = "Soma ao disponível e grava o movimento correspondente. A operação é " +
            "idempotente pelo cabeçalho `X-Idempotency-Key`: repetir a mesma chave devolve o saldo " +
            "sem somar de novo — é o que protege contra o duplo clique e contra o reenvio por " +
            "timeout. Chave nova com o mesmo conteúdo soma de novo, por ser entrada nova."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Entrada registrada, ou já registrada antes com a mesma chave. " +
            "A resposta é sempre o saldo resultante.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = SaldoDeInsumoResponse::class),
                examples = [
                    ExampleObject(
                        name = "Depois de 24 litros",
                        value = """
                        {
                          "insumoId": "018f3c10-9a12-7b44-8e01-2c5d7f8a9b31",
                          "sku": "OL-5W30-SN-1L",
                          "nome": "Óleo 5W30 sintético 1L",
                          "unidadeDeMedida": "LITRO",
                          "quantidadeDisponivel": 42.0000,
                          "quantidadeReservada": 6.0000,
                          "estoqueMinimo": 10.0000,
                          "abaixoDoMinimo": false,
                          "dataAtualizacao": "2026-09-27T13:02:44.310",
                          "reservas": [
                            {
                              "reservaId": "018f4a02-31bc-7d55-9f21-7a0c4e2b6d10",
                              "ordemServicoId": "018f4a01-1120-7e33-8a10-5c9b3d7f2e40",
                              "quantidade": 6.0000,
                              "expiraEm": "2026-09-27T18:00:00"
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
        description = "Quantidade ausente, zero, negativa, ou fracionária em unidade que não " +
            "admite fração; origem inexistente; identificador fora do formato UUID.",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class),
                examples = [
                    ExampleObject(
                        name = "Fração em unidade inteira",
                        value = """
                        {
                          "timestamp": "2026-09-27T13:05:19.442-03:00",
                          "status": 400,
                          "error": "Bad Request",
                          "code": "REGRA_DE_NEGOCIO",
                          "message": "A unidade un não admite fração; informe uma quantidade inteira",
                          "path": "/insumos/018f3c10-9a12-7b44-8e01-2c5d7f8a9b31/estoque/entradas",
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
        description = "Nenhum insumo com esse identificador.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))]
    )
    fun registrarEntrada(
        @Parameter(
            description = "Identificador do insumo.",
            example = "018f3c10-9a12-7b44-8e01-2c5d7f8a9b31",
            required = true
        )
        @PathVariable insumoId: String,
        @Parameter(
            description = "Chave de idempotência da entrada, escolhida pelo cliente. " +
                "Até 120 caracteres. Reenviar a mesma chave não soma de novo.",
            example = "nota-fiscal-88213-item-3",
            required = true
        )
        @RequestHeader(name = "X-Idempotency-Key")
        chaveIdempotencia: String,
        @Valid @RequestBody requisicao: EntradaDeEstoqueRequest,
    ): SaldoDeInsumoResponse
}
