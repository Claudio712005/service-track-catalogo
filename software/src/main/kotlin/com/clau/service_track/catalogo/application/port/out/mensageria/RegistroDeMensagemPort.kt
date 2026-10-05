package com.clau.service_track.catalogo.application.port.out.mensageria

interface RegistroDeMensagemPort {

    fun jaProcessada(tipoDaMensagem: String, chave: String): Boolean
}
