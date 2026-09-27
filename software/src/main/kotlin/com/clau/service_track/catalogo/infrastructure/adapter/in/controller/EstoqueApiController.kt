package com.clau.service_track.catalogo.infrastructure.adapter.`in`.controller

import com.clau.service_track.catalogo.application.port.`in`.api.IEstoqueApiPort
import com.clau.service_track.catalogo.application.port.`in`.api.dto.EntradaDeEstoqueRequest
import com.clau.service_track.catalogo.application.port.`in`.api.dto.SaldoDeInsumoResponse
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsultarSaldoQuery
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsultarSaldoUseCase
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.RegistrarEntradaUseCase
import com.clau.service_track.catalogo.infrastructure.adapter.`in`.mapper.EstoqueWebMapper
import org.springframework.web.bind.annotation.RestController

@RestController
class EstoqueApiController(
    private val consultarSaldo: ConsultarSaldoUseCase,
    private val registrarEntrada: RegistrarEntradaUseCase,
    private val mapper: EstoqueWebMapper,
) : IEstoqueApiPort {

    override fun consultarSaldo(insumoId: String): SaldoDeInsumoResponse =
        mapper.paraResposta(consultarSaldo.executar(ConsultarSaldoQuery(mapper.paraIdentificador(insumoId))))

    override fun registrarEntrada(
        insumoId: String,
        chaveIdempotencia: String,
        requisicao: EntradaDeEstoqueRequest,
    ): SaldoDeInsumoResponse = mapper.paraResposta(
        registrarEntrada.executar(mapper.paraComando(insumoId, chaveIdempotencia, requisicao))
    )
}
