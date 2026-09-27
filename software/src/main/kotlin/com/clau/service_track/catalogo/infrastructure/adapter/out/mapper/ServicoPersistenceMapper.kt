package com.clau.service_track.catalogo.infrastructure.adapter.out.mapper

import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import com.clau.service_track.catalogo.infrastructure.entity.postgres.ServicoEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class ServicoPersistenceMapper {

    fun paraEntidade(servico: Servico): ServicoEntity = ServicoEntity(
        id = UUID.fromString(servico.id.value),
        nomeServico = servico.nome,
        descricaoServico = servico.descricao,
        valorReferencia = servico.valorReferencia?.valor,
        ativo = servico.ativo,
        dataCriacao = paraOffset(servico.dataCriacao),
        dataAtualizacao = paraOffset(servico.dataAtualizacao),
    )

    fun atualizarEntidade(entidade: ServicoEntity, servico: Servico): ServicoEntity = entidade.apply {
        descricaoServico = servico.descricao
        valorReferencia = servico.valorReferencia?.valor
        ativo = servico.ativo
        dataAtualizacao = paraOffset(servico.dataAtualizacao)
    }

    fun paraDominio(entidade: ServicoEntity): Servico = Servico.reconstituir(
        id = DomainId.de(entidade.id.toString()),
        nome = entidade.nomeServico,
        descricao = entidade.descricaoServico,
        valorReferencia = entidade.valorReferencia?.let { ValorMonetario.de(it) },
        dataCriacao = paraLocal(entidade.dataCriacao),
        dataAtualizacao = paraLocal(entidade.dataAtualizacao),
        ativo = entidade.ativo,
    )

    private fun paraOffset(momento: LocalDateTime): OffsetDateTime = momento.atOffset(ZoneOffset.UTC)

    private fun paraLocal(momento: OffsetDateTime): LocalDateTime =
        momento.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
}
