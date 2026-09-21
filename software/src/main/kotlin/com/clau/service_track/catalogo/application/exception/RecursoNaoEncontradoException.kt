package com.clau.service_track.catalogo.application.exception

class RecursoNaoEncontradoException(
    val recurso: String,
    val identificador: String,
) : RuntimeException("$recurso não encontrado para o identificador $identificador")
