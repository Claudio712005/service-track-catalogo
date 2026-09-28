package com.clau.service_track.catalogo.application.port.out.repository

import com.clau.service_track.catalogo.domain.vo.DomainId

data class EventoParaPublicar(
    val idMensagem: String,
    val agregadoTipo: String,
    val agregadoId: DomainId,
    val chaveDeParticao: String,
    val tipoEvento: String,
    val versaoEvento: Short,
    val payload: String,
    val traceId: String?,
)
