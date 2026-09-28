package com.clau.service_track.catalogo.application.port.out.repository

import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId

interface InsumoRepositoryPort {

    fun salvar(insumo: Insumo): Insumo

    fun buscarPorId(id: DomainId): Insumo?

    fun buscarPorSku(sku: String): Insumo?

    fun listar(filtro: FiltroDeInsumo): List<Insumo>

    fun existeComSku(sku: String): Boolean

    fun buscarPorCodigoBarras(codigoBarras: String): Insumo?

    fun contarPorCategoria(categoriaId: DomainId): Long
}
