package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema

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
