package com.clau.service_track.catalogo.infrastructure.adapter.web.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(
    name = "ErrorResponse",
    description = "Corpo devolvido em toda resposta de erro deste serviço. O campo code é " +
        "estável e destinado a tratamento programático; message é destinado a leitura humana " +
        "e pode ser reescrito sem aviso."
)
data class ErrorResponse(

    @get:Schema(
        description = "Momento em que o erro foi produzido pelo servidor, com deslocamento de fuso.",
        example = "2026-09-21T15:04:22.817-03:00"
    )
    val timestamp: OffsetDateTime,

    @get:Schema(description = "Código de estado HTTP.", example = "400")
    val status: Int,

    @get:Schema(description = "Frase associada ao código de estado.", example = "Bad Request")
    val error: String,

    @get:Schema(
        description = "Classificação estável do erro. Use este campo para decidir fluxo no cliente.",
        example = "REQUISICAO_INVALIDA"
    )
    val code: CodigoErro,

    @get:Schema(
        description = "Explicação legível. Nunca contém detalhe de implementação nem dado sensível.",
        example = "Corpo da requisição contém 2 campos inválidos"
    )
    val message: String,

    @get:Schema(description = "Caminho que originou o erro.", example = "/servicos")
    val path: String,

    @get:Schema(
        description = "Identificador do trace distribuído correspondente. Informe este valor ao " +
            "reportar o problema: ele localiza a requisição inteira, atravessando os demais serviços. " +
            "Ausente quando o rastreamento está desligado.",
        example = "4bf92f3577b34da6a3ce929d0e0e4736",
        nullable = true
    )
    val traceId: String?,

    @get:Schema(
        description = "Violações por atributo. Presente apenas em erros de validação.",
        nullable = true
    )
    val violacoes: List<Violacao>? = null,
) {

    @Schema(name = "Violacao", description = "Violação de restrição em um atributo específico.")
    data class Violacao(

        @get:Schema(
            description = "Caminho do atributo, com notação de ponto para objetos aninhados.",
            example = "valorReferencia"
        )
        val campo: String,

        @get:Schema(
            description = "Restrição violada, em linguagem de negócio.",
            example = "Valor de referência não pode ser negativo"
        )
        val mensagem: String,

        @get:Schema(
            description = "Valor recebido. Omitido quando o atributo é sensível.",
            example = "-10.00",
            nullable = true
        )
        val valorRejeitado: String? = null,
    )
}
