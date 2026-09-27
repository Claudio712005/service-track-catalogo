package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(
    name = "EntradaDeEstoqueRequest",
    description = "Entrada de estoque: o que chegou na prateleira. É o único movimento que a API " +
        "aceita por HTTP — reserva, consumo e liberação nascem da saga, por mensagem no Kafka, " +
        "porque pertencem ao ciclo da ordem de serviço e não a uma ação isolada de operador."
)
data class EntradaDeEstoqueRequest(

    @field:NotNull(message = "Quantidade é obrigatória")
    @field:DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
    @field:Digits(integer = 10, fraction = 4, message = "Quantidade admite até 10 inteiros e 4 decimais")
    @get:Schema(
        description = "Quantidade que entrou, na unidade do insumo. Unidade não fracionável " +
            "(UNIDADE, PECA, CONJUNTO) exige valor inteiro.",
        example = "24",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val quantidade: BigDecimal,

    @field:DecimalMin(value = "0.00", message = "Custo unitário não pode ser negativo")
    @field:Digits(integer = 10, fraction = 2, message = "Custo unitário admite até 2 decimais")
    @get:Schema(
        description = "Custo de aquisição por unidade, quando conhecido. Fica no movimento, para " +
            "a auditoria responder quanto custou o que foi consumido.",
        example = "38.90",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val custoUnitario: BigDecimal? = null,

    @get:Schema(
        description = "Por que o estoque aumentou. NOTA_ENTRADA para compra recebida, " +
            "INVENTARIO para contagem física, AJUSTE_MANUAL para correção deliberada.",
        example = "NOTA_ENTRADA",
        allowableValues = ["NOTA_ENTRADA", "INVENTARIO", "AJUSTE_MANUAL"],
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val origemTipo: String = "NOTA_ENTRADA",

    @get:Schema(
        description = "Identificador do documento de origem — a nota fiscal, o inventário. " +
            "Guardado no movimento para a conferência posterior.",
        example = "018f3d21-77aa-7c10-9d31-4b2c8e0f1a55",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val origemId: String? = null,
)
