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

@Schema(
    name = "CategoriaResponse",
    description = "Categoria de insumo, com os atributos que ela exige dos seus insumos."
)
data class CategoriaResponse(

    @get:Schema(
        description = "Identificador da categoria. Use este valor em categoriaId ao cadastrar insumo.",
        example = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11"
    )
    val id: String,

    @get:Schema(description = "Código estável da categoria, sempre em maiúsculas.", example = "OLEO_MOTOR")
    val codigo: String,

    @get:Schema(description = "Nome exibido da categoria.", example = "Óleo de motor")
    val nome: String,

    @get:Schema(description = "Unidade sugerida para os insumos desta categoria.", example = "LITRO")
    val unidadePadrao: String,

    @get:Schema(
        description = "Se a unidade padrão admite quantidade fracionária. Derivado da unidade, " +
            "informado aqui para o cliente não precisar replicar a regra.",
        example = "true"
    )
    val unidadeFracionavel: Boolean,

    @get:Schema(
        description = "Se a categoria está ativa. Categoria desativada não aceita insumo novo, " +
            "não recebe atributo novo e os seus insumos saem da listagem padrão de insumos.",
        example = "true"
    )
    val ativa: Boolean,

    @get:Schema(description = "Atributos declarados, na ordem de cadastro.")
    val atributos: List<DefinicaoDeAtributoResponse>,
)

@Schema(
    name = "AtivacaoDeCategoriaRequest",
    description = "Liga ou desliga a categoria. Desativar preserva o histórico: os insumos " +
        "continuam existindo e consultáveis por identificador, apenas saem da listagem padrão."
)
data class AtivacaoDeCategoriaRequest(

    @get:Schema(
        description = "true reativa a categoria, false desativa.",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val ativa: Boolean,
)
