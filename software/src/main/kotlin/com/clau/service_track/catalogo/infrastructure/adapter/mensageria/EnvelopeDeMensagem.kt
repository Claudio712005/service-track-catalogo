package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

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

class MensagemInvalidaException(mensagem: String, causa: Throwable? = null) : RuntimeException(mensagem, causa)
