package com.clau.service_track.catalogo.application.port.out.repository

import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId

interface ServicoRepositoryPort {

    fun salvar(servico: Servico): Servico

    fun buscarPorId(id: DomainId): Servico?

    fun listar(incluirInativos: Boolean): List<Servico>

    fun existeComNome(nome: String): Boolean
}
