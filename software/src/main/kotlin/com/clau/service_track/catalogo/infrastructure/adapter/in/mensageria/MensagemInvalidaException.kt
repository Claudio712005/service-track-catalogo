package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mensageria

class MensagemInvalidaException(mensagem: String, causa: Throwable? = null) : RuntimeException(mensagem, causa)
