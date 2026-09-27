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
import java.time.LocalDateTime

@Schema(
    name = "CriarInsumoRequest",
    description = "Dados para cadastrar um insumo. O insumo nasce ativo e sem saldo: " +
        "quantidade em estoque é assunto de outro recurso, e não se informa aqui."
)
data class CriarInsumoRequest(

    @field:NotBlank(message = "Categoria do insumo é obrigatória")
    @get:Schema(
        description = "Identificador da categoria. A categoria determina a unidade padrão e " +
            "quais atributos o mapa especificacao aceita.",
        example = "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val categoriaId: String,

    @field:NotBlank(message = "SKU do insumo não pode ser vazio")
    @field:Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{2,39}$",
        message = "SKU admite de 3 a 40 caracteres entre letras, dígitos, ponto, hífen e sublinhado"
    )
    @get:Schema(
        description = "Código interno do insumo, normalizado para maiúsculas pelo servidor. " +
            "Único entre todos os insumos, ativos ou não, e imutável depois do cadastro: " +
            "ele aparece em ordem de serviço antiga e em etiqueta de prateleira.",
        example = "OL-5W30-SYN-1L",
        maxLength = 40,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val sku: String,

    @field:NotBlank(message = "Nome do insumo não pode ser vazio")
    @field:Size(max = 160, message = "Nome excede 160 caracteres")
    @get:Schema(
        description = "Nome comercial do material, como aparece no orçamento ao cliente.",
        example = "Óleo 5W30 sintético 1L",
        maxLength = 160,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val nome: String,

    @field:NotBlank(message = "Descrição do insumo não pode ser vazia")
    @get:Schema(
        description = "Descrição técnica do material, para o mecânico conferir aplicação.",
        example = "Óleo lubrificante sintético para motores a gasolina e flex, embalagem de 1 litro.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val descricao: String,

    @field:NotNull(message = "Custo do insumo é obrigatório")
    @field:DecimalMin(value = "0.00", message = "Custo não pode ser negativo")
    @field:Digits(integer = 10, fraction = 2, message = "Custo admite no máximo duas casas decimais")
    @get:Schema(
        description = "Custo de tabela por unidade de medida, em reais. É referência de compra: " +
            "o custo efetivo de cada entrada fica no lote, quando o insumo controla lote.",
        example = "38.90",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val custo: BigDecimal,

    @get:Schema(
        description = "Unidade de movimentação. Omita para herdar a unidade padrão da categoria. " +
            "Depois do primeiro movimento de estoque ela não muda mais, porque alteraria o " +
            "significado das quantidades já registradas.",
        example = "LITRO",
        allowableValues = ["UNIDADE", "PECA", "LITRO", "MILILITRO", "GALAO", "QUILOGRAMA", "GRAMA", "METRO", "CONJUNTO"],
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val unidadeDeMedida: String? = null,

    @get:Schema(
        description = "Características específicas deste insumo, no formato chave e valor em texto. " +
            "As chaves aceitas são exatamente as declaradas pela categoria: chave desconhecida é " +
            "recusada com 400, e atributo obrigatório ausente também. O valor vai como texto e é " +
            "convertido conforme o tipo declarado — \"98\" em atributo INTEIRO vira número, " +
            "\"sim\" em BOOLEANO vira verdadeiro.",
        example = "{\"viscosidade\": \"5W30\", \"especificacao-api\": \"SN\", \"sintetico\": \"sim\"}",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val especificacao: Map<String, String> = emptyMap(),

    @field:Size(max = 80, message = "Marca excede 80 caracteres")
    @get:Schema(description = "Marca do produto.", example = "Lubrax", nullable = true)
    val marca: String? = null,

    @field:Size(max = 120, message = "Fabricante excede 120 caracteres")
    @get:Schema(
        description = "Fabricante, quando diferente da marca.",
        example = "Petrobras",
        nullable = true
    )
    val fabricante: String? = null,

    @field:Size(max = 60, message = "Código do fabricante excede 60 caracteres")
    @get:Schema(
        description = "Código do item no catálogo do fabricante, usado na cotação com fornecedor.",
        example = "LB-5W30-1L",
        nullable = true
    )
    val codigoFabricante: String? = null,

    @field:Pattern(regexp = "^[0-9]{8,14}$", message = "Código de barras admite de 8 a 14 dígitos")
    @get:Schema(
        description = "EAN ou GTIN, para leitura por scanner no balcão. Único entre os insumos " +
            "quando informado.",
        example = "7891234567890",
        nullable = true
    )
    val codigoBarras: String? = null,

    @get:Schema(
        description = "Verdadeiro para material que exige controle de lote e validade na entrada, " +
            "como óleo, fluido e filtro com prazo. Reflete no estoque quando ele entrar em operação.",
        example = "true",
        defaultValue = "false"
    )
    val controlaLote: Boolean = false,

    @field:Min(value = 0, message = "Prazo de validade não pode ser negativo")
    @get:Schema(
        description = "Prazo de validade típico em dias, usado para sugerir a data de vencimento " +
            "na entrada do lote. Informe apenas para material que vence.",
        example = "730",
        nullable = true
    )
    val validadeEmDias: Int? = null,
)

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
