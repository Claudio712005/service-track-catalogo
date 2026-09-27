package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.mongo

import com.clau.service_track.catalogo.application.port.out.repository.FiltroDeInsumo
import com.clau.service_track.catalogo.application.port.out.repository.InsumoRepositoryPort
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.CategoriaPersistenceMapper
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.InsumoPersistenceMapper
import com.clau.service_track.catalogo.infrastructure.entity.mongo.InsumoDocumento
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.util.regex.Pattern

@Repository
@Profile("!teste")
class InsumoRepositoryMongoAdapter(
    private val colecao: InsumoMongoRepository,
    private val categorias: CategoriaMongoRepository,
    private val mapper: InsumoPersistenceMapper,
    private val mapperDeCategoria: CategoriaPersistenceMapper,
    private val template: MongoTemplate,
) : InsumoRepositoryPort {

    override fun salvar(insumo: Insumo): Insumo {
        val existente = colecao.findById(insumo.id.value).orElse(null)
        val documento = if (existente == null) {
            mapper.paraDocumento(insumo)
        } else {
            mapper.atualizarDocumento(existente, insumo)
        }
        return paraDominio(colecao.save(documento))
    }

    override fun buscarPorId(id: DomainId): Insumo? = colecao
        .findById(id.value)
        .map(::paraDominio)
        .orElse(null)

    override fun buscarPorSku(sku: String): Insumo? = colecao.findBySku(sku)?.let(::paraDominio)

    override fun buscarPorCodigoBarras(codigoBarras: String): Insumo? =
        colecao.findByCodigoBarras(codigoBarras)?.let(::paraDominio)

    override fun listar(filtro: FiltroDeInsumo): List<Insumo> {
        val criterios = mutableListOf<Criteria>()

        if (!filtro.incluirInativos) {
            criterios += Criteria.where("ATIVO").`is`(true)
            idsDeCategoriasDesativadas().takeIf { it.isNotEmpty() }?.let {
                criterios += Criteria.where("CATEGORIA_ID").nin(it)
            }
        }
        filtro.categoriaId?.let { criterios += Criteria.where("CATEGORIA_ID").`is`(it.value) }
        filtro.termo?.let { termo ->
            val expressao = Pattern.quote(termo)
            criterios += Criteria().orOperator(
                Criteria.where("SKU").regex(expressao, "i"),
                Criteria.where("NOME").regex(expressao, "i"),
                Criteria.where("MARCA").regex(expressao, "i"),
            )
        }

        val consulta = if (criterios.isEmpty()) Query() else Query(Criteria().andOperator(criterios))
        val documentos = template.find(consulta, InsumoDocumento::class.java)
        return paraDominio(documentos)
    }

    override fun existeComSku(sku: String): Boolean = colecao.existsBySku(sku)

    override fun contarPorCategoria(categoriaId: DomainId): Long = colecao.countByCategoriaId(categoriaId.value)

    private fun paraDominio(documento: InsumoDocumento): Insumo =
        mapper.paraDominio(documento, categoriaDe(documento.categoriaId))

    private fun paraDominio(documentos: List<InsumoDocumento>): List<Insumo> {
        if (documentos.isEmpty()) return emptyList()

        val porId = categorias.findAllById(documentos.map { it.categoriaId }.distinct())
            .associate { it.id to mapperDeCategoria.paraDominio(it) }

        return documentos.map { mapper.paraDominio(it, porId[it.categoriaId]) }
    }

    private fun idsDeCategoriasDesativadas(): List<String> = categorias.findAllByAtiva(false).map { it.id }

    private fun categoriaDe(categoriaId: String): CategoriaDeInsumo? = categorias
        .findById(categoriaId)
        .map(mapperDeCategoria::paraDominio)
        .orElse(null)
}
