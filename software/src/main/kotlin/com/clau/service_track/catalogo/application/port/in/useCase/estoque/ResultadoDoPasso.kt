package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo

data class ResultadoDoPasso(
    val situacao: SituacaoDoPasso,
    val saldo: SaldoDeInsumo?,
    val motivo: String? = null,
) {

    companion object {
        fun aplicado(saldo: SaldoDeInsumo) = ResultadoDoPasso(SituacaoDoPasso.APLICADO, saldo)
        fun jaProcessado() = ResultadoDoPasso(SituacaoDoPasso.JA_PROCESSADO, null)
        fun recusado(motivo: String) = ResultadoDoPasso(SituacaoDoPasso.RECUSADO, null, motivo)
    }
}
