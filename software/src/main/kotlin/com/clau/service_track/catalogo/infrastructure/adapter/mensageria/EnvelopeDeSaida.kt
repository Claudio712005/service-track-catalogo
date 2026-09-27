package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

import java.time.OffsetDateTime

data class EnvelopeDeSaida(
    val idMensagem: String,
    val tipo: String,
    val versao: Short,
    val ocorridoEm: OffsetDateTime,
    val correlationId: String?,
    val traceId: String?,
    val dados: Any,
)
