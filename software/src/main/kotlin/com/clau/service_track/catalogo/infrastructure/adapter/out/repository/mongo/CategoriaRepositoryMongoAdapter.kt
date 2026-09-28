package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.mongo

import com.clau.service_track.catalogo.application.port.out.repository.CategoriaRepositoryPort
import com.clau.service_track.catalogo.domain.model.CategoriaDeInsumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.CategoriaPersistenceMapper
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("!teste")
class CategoriaRepositoryMongoAdapter(
    private val colecao: CategoriaMongoRepository,
    private val mapper: CategoriaPersistenceMapper,
) : CategoriaRepositoryPort {

    override fun salvar(categoria: CategoriaDeInsumo): CategoriaDeInsumo {
        val criadoEm = colecao.findById(categoria.id.value).orElse(null)?.dataCriacao
        val documento = mapper.paraDocumento(categoria, criadoEm)
        return mapper.paraDominio(colecao.save(documento))
    }

    override fun buscarPorId(id: DomainId): CategoriaDeInsumo? = colecao
        .findById(id.value)
        .map(mapper::paraDominio)
        .orElse(null)

    override fun buscarPorIds(ids: Collection<DomainId>): Map<DomainId, CategoriaDeInsumo> {
        if (ids.isEmpty()) return emptyMap()
        return colecao.findAllById(ids.map { it.value })
            .map(mapper::paraDominio)
            .associateBy { it.id }
    }

    override fun buscarPorCodigo(codigo: String): CategoriaDeInsumo? = colecao
        .findByCodigo(codigo.trim().uppercase())
        ?.let(mapper::paraDominio)

    override fun listar(termo: String?, incluirDesativadas: Boolean): List<CategoriaDeInsumo> {
        val documentos = if (termo == null) {
            colecao.findAll()
        } else {
            colecao.findByCodigoContainingIgnoreCaseOrNomeContainingIgnoreCase(termo, termo)
        }
        return documentos
            .filter { incluirDesativadas || it.ativa }
            .map(mapper::paraDominio)
    }

    override fun existeComCodigo(codigo: String): Boolean = colecao.existsByCodigo(codigo.trim().uppercase())
}
