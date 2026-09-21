package com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper

import com.clau.service_track.catalogo.application.port.`in`.api.dto.AtualizarServicoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.CriarServicoRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.ServicoResponse
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.AtualizarServicoCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.servico.CriarServicoCommand
import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ServicoWebMapper {

    fun paraIdentificador(bruto: String): DomainId {
        val normalizado = runCatching { UUID.fromString(bruto.trim()) }
            .getOrElse { throw DomainException("Identificador '$bruto' não é um UUID válido") }
        return DomainId.de(normalizado.toString())
    }

    fun paraComando(requisicao: CriarServicoRequest) = CriarServicoCommand(
        nome = requisicao.nome.trim(),
        descricao = requisicao.descricao.trim(),
        valorReferencia = requisicao.valorReferencia?.let { ValorMonetario.de(it) },
    )

    fun paraComando(id: String, requisicao: AtualizarServicoRequest) = AtualizarServicoCommand(
        id = paraIdentificador(id),
        descricao = requisicao.descricao.trim(),
        valorReferencia = requisicao.valorReferencia?.let { ValorMonetario.de(it) },
    )

    fun paraResposta(servico: Servico) = ServicoResponse(
        id = servico.id.value,
        nome = servico.nome,
        descricao = servico.descricao,
        valorReferencia = servico.valorReferencia?.valor,
        ativo = servico.ativo,
        dataCriacao = servico.dataCriacao,
        dataAtualizacao = servico.dataAtualizacao,
    )

    fun paraResposta(servicos: List<Servico>) = servicos.map(::paraResposta)
}
