package com.clau.service_track.catalogo.application.port.out.repository

import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId

interface CategoriaRepositoryPort {

    fun salvar(categoria: CategoriaDeInsumo): CategoriaDeInsumo

    fun buscarPorId(id: DomainId): CategoriaDeInsumo?

    fun buscarPorIds(ids: Collection<DomainId>): Map<DomainId, CategoriaDeInsumo>

    fun buscarPorCodigo(codigo: String): CategoriaDeInsumo?

    fun listar(termo: String?): List<CategoriaDeInsumo>

    fun existeComCodigo(codigo: String): Boolean
}
