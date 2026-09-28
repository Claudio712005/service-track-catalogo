package com.clau.service_track.catalogo.application.handler.estoque

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsultarSaldoQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsultarSaldoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ResultadoDeSaldo
import com.clau.service_track.catalogo.application.port.out.repository.EstoqueRepositoryPort
import com.clau.service_track.catalogo.application.port.out.repository.InsumoRepositoryPort
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.shared.annotation.UseCase

@UseCase
class EstoqueQueryHandler(
    private val estoque: EstoqueRepositoryPort,
    private val insumos: InsumoRepositoryPort,
) : ConsultarSaldoUseCase {

    override fun executar(consulta: ConsultarSaldoQuery): ResultadoDeSaldo {
        val insumo = insumos.buscarPorId(consulta.insumoId)
            ?: throw RecursoNaoEncontradoException("Insumo", consulta.insumoId.value)

        val saldo = estoque.buscarSaldo(insumo.id) ?: SaldoDeInsumo.zerado(insumo.id, insumo.unidadeDeMedida)
        return ResultadoDeSaldo(insumo, saldo)
    }
}
