package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria.dto

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime

data class EnvelopeDeMensagem(
    val idMensagem: String,
    val tipo: String,
    val versao: Short = 1,
    val ocorridoEm: OffsetDateTime,
    val correlationId: String?,
    val traceId: String?,
    val dados: JsonNode,
)
