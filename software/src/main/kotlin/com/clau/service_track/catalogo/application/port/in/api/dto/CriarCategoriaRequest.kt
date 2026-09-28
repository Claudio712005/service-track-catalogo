package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(
    name = "CriarCategoriaRequest",
    description = "Dados para criar uma categoria de insumo. A categoria define a unidade padrão " +
        "e o conjunto de atributos que seus insumos aceitam — é o metadado que permite um óleo " +
        "ter viscosidade e um pneu ter índice de carga, sem coluna nova no banco."
)
data class CriarCategoriaRequest(

    @field:NotBlank(message = "Código da categoria não pode ser vazio")
    @field:Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9_-]*$",
        message = "Código admite letras, dígitos, hífen e sublinhado, começando por letra"
    )
    @field:Size(max = 40, message = "Código excede 40 caracteres")
    @get:Schema(
        description = "Código estável da categoria, normalizado para maiúsculas pelo servidor. " +
            "Único entre as categorias.",
        example = "OLEO_MOTOR",
        maxLength = 40,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val codigo: String,

    @field:NotBlank(message = "Nome da categoria não pode ser vazio")
    @field:Size(max = 120, message = "Nome excede 120 caracteres")
    @get:Schema(
        description = "Nome exibido nas telas de cadastro e de consulta.",
        example = "Óleo de motor",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val nome: String,

    @get:Schema(
        description = "Unidade sugerida ao cadastrar insumo desta categoria. O insumo pode " +
            "declarar outra, quando justificado. Unidades fracionáveis aceitam quantidade decimal; " +
            "UNIDADE, PECA e CONJUNTO exigem quantidade inteira.",
        example = "LITRO",
        allowableValues = ["UNIDADE", "PECA", "LITRO", "MILILITRO", "GALAO", "QUILOGRAMA", "GRAMA", "METRO", "CONJUNTO"],
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val unidadePadrao: String,

    @field:Valid
    @get:Schema(
        description = "Atributos que os insumos da categoria aceitam. Pode nascer vazia e receber " +
            "atributos depois, por POST /categorias/{id}/atributos.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val atributos: List<DefinicaoDeAtributoRequest> = emptyList(),
)
