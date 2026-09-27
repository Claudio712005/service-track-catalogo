package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.application.port.out.repository.ServicoRepositoryPort
import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.infrastructure.adapter.out.mapper.ServicoPersistenceMapper
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Profile("!teste")
class ServicoRepositoryPostgresAdapter(
    private val repositorio: ServicoJpaRepository,
    private val mapper: ServicoPersistenceMapper,
) : ServicoRepositoryPort {

    @Transactional
    override fun salvar(servico: Servico): Servico {
        val identificador = UUID.fromString(servico.id.value)
        val existente = repositorio.findById(identificador).orElse(null)

        val entidade = if (existente == null) {
            mapper.paraEntidade(servico)
        } else {
            mapper.atualizarEntidade(existente, servico)
        }

        return mapper.paraDominio(repositorio.save(entidade))
    }

    @Transactional(readOnly = true)
    override fun buscarPorId(id: DomainId): Servico? = repositorio
        .findById(UUID.fromString(id.value))
        .map(mapper::paraDominio)
        .orElse(null)

    @Transactional(readOnly = true)
    override fun listar(incluirInativos: Boolean): List<Servico> {
        val entidades = if (incluirInativos) repositorio.findAll() else repositorio.findAllByAtivoTrue()
        return entidades.map(mapper::paraDominio)
    }

    @Transactional(readOnly = true)
    override fun existeComNome(nome: String): Boolean =
        repositorio.existsByNomeServicoIgnoreCaseAndAtivoTrue(nome.trim())
}
