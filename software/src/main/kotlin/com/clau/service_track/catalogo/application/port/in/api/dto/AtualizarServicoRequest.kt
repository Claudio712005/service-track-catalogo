package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

@Schema(
    name = "AtualizarServicoRequest",
    description = "Campos alteráveis de um serviço já cadastrado. O nome não consta " +
        "porque é imutável após o cadastro."
)
data class AtualizarServicoRequest(

    @field:NotBlank(message = "Descrição do serviço não pode ser vazia")
    @get:Schema(
        description = "Nova descrição. Substitui integralmente a anterior.",
        example = "Substituição do óleo lubrificante do motor, do filtro de óleo e do filtro de ar.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val descricao: String,

    @field:DecimalMin(value = "0.00", message = "Valor de referência não pode ser negativo")
    @field:Digits(integer = 10, fraction = 2, message = "Valor de referência admite no máximo duas casas decimais")
    @get:Schema(
        description = "Novo valor de referência. Orçamentos já emitidos não são recalculados.",
        example = "209.90",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val valorReferencia: BigDecimal? = null,
)
