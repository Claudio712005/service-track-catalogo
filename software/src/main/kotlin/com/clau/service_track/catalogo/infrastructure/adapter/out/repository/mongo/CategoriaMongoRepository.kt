package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.mongo

import com.clau.service_track.catalogo.infrastructure.entity.mongo.CategoriaDocumento
import org.springframework.data.mongodb.repository.MongoRepository

interface CategoriaMongoRepository : MongoRepository<CategoriaDocumento, String> {

    fun findByCodigo(codigo: String): CategoriaDocumento?

    fun existsByCodigo(codigo: String): Boolean

    fun findAllByAtiva(ativa: Boolean): List<CategoriaDocumento>

    fun findByCodigoContainingIgnoreCaseOrNomeContainingIgnoreCase(
        codigo: String,
        nome: String,
    ): List<CategoriaDocumento>
}
