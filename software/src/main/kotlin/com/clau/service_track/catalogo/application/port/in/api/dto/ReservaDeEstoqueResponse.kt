package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(
    name = "ReservaDeEstoqueResponse",
    description = "Reserva ativa de um insumo para uma ordem de serviço."
)
data class ReservaDeEstoqueResponse(

    @get:Schema(description = "Identificador da reserva.", example = "018f4a02-31bc-7d55-9f21-7a0c4e2b6d10")
    val reservaId: String,

    @get:Schema(
        description = "Ordem de serviço que detém a reserva.",
        example = "018f4a01-1120-7e33-8a10-5c9b3d7f2e40"
    )
    val ordemServicoId: String,

    @get:Schema(description = "Quantidade reservada.", example = "6.0000")
    val quantidade: BigDecimal,

    @get:Schema(
        description = "Quando a reserva vence, em UTC. Vencida e não resolvida pela saga, é " +
            "devolvida ao disponível pela rotina de expiração. Nulo significa sem prazo.",
        example = "2026-09-27T18:00:00"
    )
    val expiraEm: String?,
)
