package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema

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
