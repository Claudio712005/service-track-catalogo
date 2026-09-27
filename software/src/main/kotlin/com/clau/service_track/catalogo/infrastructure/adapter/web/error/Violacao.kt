package com.clau.service_track.catalogo.infrastructure.adapter.web.error

import io.swagger.v3.oas.annotations.media.Schema

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
