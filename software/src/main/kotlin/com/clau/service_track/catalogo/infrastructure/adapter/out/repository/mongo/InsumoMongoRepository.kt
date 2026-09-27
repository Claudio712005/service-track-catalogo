package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.mongo

import com.clau.service_track.catalogo.infrastructure.entity.mongo.InsumoDocumento
import org.springframework.data.mongodb.repository.MongoRepository

interface InsumoMongoRepository : MongoRepository<InsumoDocumento, String> {

    fun findBySku(sku: String): InsumoDocumento?

    fun existsBySku(sku: String): Boolean

    fun findByCodigoBarras(codigoBarras: String): InsumoDocumento?

    fun countByCategoriaId(categoriaId: String): Long
}
