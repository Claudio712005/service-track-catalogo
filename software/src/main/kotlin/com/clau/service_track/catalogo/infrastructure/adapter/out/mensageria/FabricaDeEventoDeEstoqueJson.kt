package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria

import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ConsumirReservaCommand
import com.clau.service_track.catalogo.application.port.`in`.useCase.estoque.ReservarEstoqueCommand
import com.clau.service_track.catalogo.application.port.out.mensageria.FabricaDeEventoDeEstoquePort
import com.clau.service_track.catalogo.application.port.out.repository.EventoParaPublicar
import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.model.ResultadoDeReserva
import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo
import com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto.DadosDeConsumoRecusado
import com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto.DadosDeReservaDeEstoque
import com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto.DadosDeReservaRecusada
import com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto.EnvelopeDeSaida
import com.clau.service_track.catalogo.infrastructure.adapter.web.filter.CorrelacaoFilter
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import org.slf4j.MDC
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class FabricaDeEventoDeEstoqueJson(
    private val mapper: ObjectMapper,
) : FabricaDeEventoDeEstoquePort {

    override fun estoqueReservado(insumo: Insumo, resultado: ResultadoDeReserva, saldo: SaldoDeInsumo, traceId: String?) =
        evento(ESTOQUE_RESERVADO, insumo, resultado, saldo, traceId)

    override fun estoqueConsumido(insumo: Insumo, resultado: ResultadoDeReserva, saldo: SaldoDeInsumo, traceId: String?) =
        evento(ESTOQUE_CONSUMIDO, insumo, resultado, saldo, traceId)

    override fun reservaLiberada(insumo: Insumo, resultado: ResultadoDeReserva, saldo: SaldoDeInsumo, traceId: String?) =
        evento(RESERVA_LIBERADA, insumo, resultado, saldo, traceId)

    override fun reservaExpirada(insumo: Insumo, resultado: ResultadoDeReserva, saldo: SaldoDeInsumo, traceId: String?) =
        evento(RESERVA_EXPIRADA, insumo, resultado, saldo, traceId)

    override fun reservaRecusada(
        insumo: Insumo,
        comando: ReservarEstoqueCommand,
        motivo: String,
    ): EventoParaPublicar {
        val dados = DadosDeReservaRecusada(
            insumoId = insumo.id.value,
            sku = insumo.sku,
            ordemServicoId = comando.ordemServicoId.value,
            quantidadeSolicitada = comando.quantidade,
            unidadeDeMedida = insumo.unidadeDeMedida.name,
            motivo = motivo,
        )
        return empacotar(RESERVA_RECUSADA, insumo, comando.ordemServicoId.value, dados, comando.traceId)
    }

    override fun consumoRecusado(
        insumo: Insumo,
        comando: ConsumirReservaCommand,
        motivo: String,
    ): EventoParaPublicar {
        val dados = DadosDeConsumoRecusado(
            insumoId = insumo.id.value,
            sku = insumo.sku,
            ordemServicoId = comando.ordemServicoId.value,
            motivo = motivo,
        )
        return empacotar(CONSUMO_RECUSADO, insumo, comando.ordemServicoId.value, dados, comando.traceId)
    }

    private fun evento(
        tipo: String,
        insumo: Insumo,
        resultado: ResultadoDeReserva,
        saldo: SaldoDeInsumo,
        traceId: String?,
    ): EventoParaPublicar {
        val dados = DadosDeReservaDeEstoque(
            insumoId = insumo.id.value,
            sku = insumo.sku,
            ordemServicoId = resultado.reserva.ordemServicoId.value,
            reservaId = resultado.reserva.id.value,
            quantidade = resultado.reserva.quantidade,
            unidadeDeMedida = insumo.unidadeDeMedida.name,
            expiraEm = resultado.reserva.expiraEm?.atZone(ZoneId.systemDefault())?.toOffsetDateTime(),
            saldoDisponivel = saldo.quantidadeDisponivel,
            saldoReservado = saldo.quantidadeReservada,
        )
        return empacotar(tipo, insumo, resultado.reserva.ordemServicoId.value, dados, traceId)
    }

    private fun empacotar(
        tipo: String,
        insumo: Insumo,
        chaveDeParticao: String,
        dados: Any,
        traceId: String?,
    ): EventoParaPublicar {
        val idMensagem = UUID.randomUUID().toString()
        val envelope = EnvelopeDeSaida(
            idMensagem = idMensagem,
            tipo = tipo,
            versao = VERSAO_DO_CONTRATO,
            ocorridoEm = OffsetDateTime.now(ZoneOffset.UTC),
            correlationId = MDC.get(CorrelacaoFilter.CHAVE_CORRELACAO),
            traceId = traceId,
            dados = dados,
        )

        return EventoParaPublicar(
            idMensagem = idMensagem,
            agregadoTipo = AGREGADO,
            agregadoId = insumo.id,
            chaveDeParticao = chaveDeParticao,
            tipoEvento = tipo,
            versaoEvento = VERSAO_DO_CONTRATO,
            payload = mapper.writeValueAsString(envelope),
            traceId = traceId,
        )
    }

    private companion object {
        const val AGREGADO = "SaldoDeInsumo"
        const val VERSAO_DO_CONTRATO: Short = 1
        const val ESTOQUE_RESERVADO = "EstoqueReservado"
        const val ESTOQUE_CONSUMIDO = "EstoqueConsumido"
        const val RESERVA_LIBERADA = "ReservaLiberada"
        const val RESERVA_EXPIRADA = "ReservaExpirada"
        const val RESERVA_RECUSADA = "ReservaRecusada"
        const val CONSUMO_RECUSADO = "ConsumoRecusado"
    }
}
