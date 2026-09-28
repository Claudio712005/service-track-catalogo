package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(
    name = "AtualizarInsumoRequest",
    description = "Campos alteráveis de um insumo. SKU, nome, categoria e unidade de medida não " +
        "constam porque são imutáveis após o cadastro: os três primeiros identificam o material " +
        "em documento já emitido, e a unidade daria outro significado às quantidades registradas. " +
        "A substituição é integral — o que não vier no corpo é apagado, inclusive atributos."
)
data class AtualizarInsumoRequest(

    @field:NotBlank(message = "Descrição do insumo não pode ser vazia")
    @get:Schema(
        description = "Nova descrição técnica. Substitui integralmente a anterior.",
        example = "Óleo lubrificante sintético para motores a gasolina, flex e GNV, embalagem de 1 litro.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val descricao: String,

    @field:NotNull(message = "Custo do insumo é obrigatório")
    @field:DecimalMin(value = "0.00", message = "Custo não pode ser negativo")
    @field:Digits(integer = 10, fraction = 2, message = "Custo admite no máximo duas casas decimais")
    @get:Schema(
        description = "Novo custo de tabela. Orçamentos já emitidos não são recalculados: " +
            "eles guardam o custo vigente na data de emissão.",
        example = "41.50",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val custo: BigDecimal,

    @get:Schema(
        description = "Especificação completa, validada contra a categoria do insumo. " +
            "Atributo omitido aqui é removido do insumo; atributo obrigatório da categoria " +
            "não pode ser omitido.",
        example = "{\"viscosidade\": \"5W30\", \"especificacao-api\": \"SP\", \"sintetico\": \"sim\"}",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val especificacao: Map<String, String> = emptyMap(),

    @field:Size(max = 80, message = "Marca excede 80 caracteres")
    @get:Schema(description = "Marca do produto. Nulo remove a marca registrada.", example = "Lubrax", nullable = true)
    val marca: String? = null,

    @field:Size(max = 120, message = "Fabricante excede 120 caracteres")
    @get:Schema(description = "Fabricante. Nulo remove o valor registrado.", example = "Petrobras", nullable = true)
    val fabricante: String? = null,

    @field:Size(max = 60, message = "Código do fabricante excede 60 caracteres")
    @get:Schema(description = "Código no catálogo do fabricante.", example = "LB-5W30-1L", nullable = true)
    val codigoFabricante: String? = null,

    @field:Pattern(regexp = "^[0-9]{8,14}$", message = "Código de barras admite de 8 a 14 dígitos")
    @get:Schema(
        description = "EAN ou GTIN. Recusado com 409 se já pertencer a outro insumo.",
        example = "7891234567890",
        nullable = true
    )
    val codigoBarras: String? = null,

    @get:Schema(description = "Se o material exige controle de lote e validade.", example = "true", defaultValue = "false")
    val controlaLote: Boolean = false,

    @field:Min(value = 0, message = "Prazo de validade não pode ser negativo")
    @get:Schema(description = "Prazo de validade típico em dias.", example = "730", nullable = true)
    val validadeEmDias: Int? = null,
)
