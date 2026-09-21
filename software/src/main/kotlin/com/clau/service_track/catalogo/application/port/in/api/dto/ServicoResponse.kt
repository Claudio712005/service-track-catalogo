package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(
    name = "ServicoResponse",
    description = "Serviço oferecido pela oficina, como consta no catálogo."
)
data class ServicoResponse(

    @get:Schema(
        description = "Identificador do serviço. Estável por toda a vida do registro, inclusive após desativação.",
        example = "bbfdb1a8-66e2-4292-a6cb-6a6d3fb080fd"
    )
    val id: String,

    @get:Schema(
        description = "Nome comercial do serviço. Imutável após o cadastro.",
        example = "Troca de óleo e filtro"
    )
    val nome: String,

    @get:Schema(
        description = "Descrição do que o serviço contempla.",
        example = "Substituição do óleo lubrificante do motor e do filtro de óleo, com verificação de nível dos demais fluidos e descarte do material usado."
    )
    val descricao: String,

    @get:Schema(
        description = "Valor de referência usado para compor orçamentos. Ausente quando o serviço " +
            "é orçado caso a caso. Não representa preço final ao cliente.",
        example = "189.90",
        nullable = true
    )
    val valorReferencia: BigDecimal?,

    @get:Schema(
        description = "Serviços desativados permanecem consultáveis por identificador, " +
            "mas são omitidos das listagens e não podem compor novos orçamentos.",
        example = "true"
    )
    val ativo: Boolean,

    @get:Schema(description = "Momento do cadastro.", example = "2026-03-12T09:14:00")
    val dataCriacao: LocalDateTime,

    @get:Schema(description = "Momento da última alteração.", example = "2026-03-12T09:14:00")
    val dataAtualizacao: LocalDateTime,
)
