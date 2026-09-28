package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(
    name = "InsumoResponse",
    description = "Insumo como consta no catálogo. Não traz quantidade em estoque: saldo, reserva " +
        "e movimentação são de outro recurso, e o catálogo responde apenas o que o material é."
)
data class InsumoResponse(

    @get:Schema(
        description = "Identificador do insumo. Estável por toda a vida do registro, e é o valor " +
            "que a ordem de serviço guarda como referência.",
        example = "018f30bb-77a1-7c22-9b10-2a44de81f0aa"
    )
    val id: String,

    @get:Schema(description = "Identificador da categoria do insumo.", example = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11")
    val categoriaId: String,

    @get:Schema(description = "Código interno, sempre em maiúsculas. Imutável.", example = "OL-5W30-SYN-1L")
    val sku: String,

    @get:Schema(description = "Nome comercial do material.", example = "Óleo 5W30 sintético 1L")
    val nome: String,

    @get:Schema(
        description = "Descrição técnica do material.",
        example = "Óleo lubrificante sintético para motores a gasolina e flex, embalagem de 1 litro."
    )
    val descricao: String,

    @get:Schema(description = "Unidade de movimentação.", example = "LITRO")
    val unidadeDeMedida: String,

    @get:Schema(
        description = "Se a unidade admite quantidade fracionária.",
        example = "true"
    )
    val unidadeFracionavel: Boolean,

    @get:Schema(description = "Custo de tabela por unidade, em reais.", example = "38.90")
    val custo: BigDecimal,

    @get:Schema(
        description = "Características específicas, convertidas para o tipo declarado pela categoria " +
            "e devolvidas em texto. Chaves são as da categoria.",
        example = "{\"viscosidade\": \"5W30\", \"especificacao-api\": \"SN\", \"sintetico\": \"true\"}"
    )
    val especificacao: Map<String, String>,

    @get:Schema(description = "Marca do produto.", example = "Lubrax", nullable = true)
    val marca: String?,

    @get:Schema(description = "Fabricante.", example = "Petrobras", nullable = true)
    val fabricante: String?,

    @get:Schema(description = "Código no catálogo do fabricante.", example = "LB-5W30-1L", nullable = true)
    val codigoFabricante: String?,

    @get:Schema(description = "EAN ou GTIN.", example = "7891234567890", nullable = true)
    val codigoBarras: String?,

    @get:Schema(description = "Se o material exige controle de lote e validade na entrada.", example = "true")
    val controlaLote: Boolean,

    @get:Schema(description = "Prazo de validade típico em dias.", example = "730", nullable = true)
    val validadeEmDias: Int?,

    @get:Schema(
        description = "Insumo desativado permanece consultável por identificador e continua " +
            "referenciado por ordens antigas, mas sai das listagens e não entra em orçamento novo.",
        example = "true"
    )
    val ativo: Boolean,

    @get:Schema(description = "Momento do cadastro.", example = "2026-09-26T10:15:00")
    val dataCriacao: LocalDateTime,

    @get:Schema(description = "Momento da última alteração.", example = "2026-09-26T10:15:00")
    val dataAtualizacao: LocalDateTime,
)
