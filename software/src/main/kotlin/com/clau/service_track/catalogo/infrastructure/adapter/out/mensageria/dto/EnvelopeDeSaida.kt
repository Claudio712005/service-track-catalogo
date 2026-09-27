package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto

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
