package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(
    name = "CriarServicoRequest",
    description = "Dados para cadastrar um serviço no catálogo."
)
data class CriarServicoRequest(

    @field:NotBlank(message = "Nome do serviço não pode ser vazio")
    @field:Size(max = 150, message = "Nome do serviço excede 150 caracteres")
    @get:Schema(
        description = "Nome comercial do serviço. Único entre os serviços ativos, e imutável " +
            "após o cadastro: correções exigem desativar o registro e cadastrar outro.",
        example = "Troca de óleo e filtro",
        maxLength = 150,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val nome: String,

    @field:NotBlank(message = "Descrição do serviço não pode ser vazia")
    @get:Schema(
        description = "Descrição do que o serviço contempla. Aparece ao cliente no orçamento.",
        example = "Substituição do óleo lubrificante do motor e do filtro de óleo, com verificação de nível dos demais fluidos e descarte do material usado.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val descricao: String,

    @field:DecimalMin(value = "0.00", message = "Valor de referência não pode ser negativo")
    @field:Digits(integer = 10, fraction = 2, message = "Valor de referência admite no máximo duas casas decimais")
    @get:Schema(
        description = "Valor de referência para composição de orçamento. " +
            "Omita quando o serviço for orçado caso a caso. Valores com mais de duas casas " +
            "decimais são rejeitados, não arredondados.",
        example = "189.90",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val valorReferencia: BigDecimal? = null,
)
