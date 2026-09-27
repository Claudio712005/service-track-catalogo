package com.clau.service_track.catalogo.application.port.`in`.useCase.estoque

import com.clau.service_track.catalogo.domain.model.SaldoDeInsumo

data class ResultadoDoPasso(
    val situacao: Situacao,
    val saldo: SaldoDeInsumo?,
    val motivo: String? = null,
) {

    enum class Situacao {
        APLICADO,
        JA_PROCESSADO,
        RECUSADO,
    }

    companion object {
        fun aplicado(saldo: SaldoDeInsumo) = ResultadoDoPasso(Situacao.APLICADO, saldo)
        fun jaProcessado() = ResultadoDoPasso(Situacao.JA_PROCESSADO, null)
        fun recusado(motivo: String) = ResultadoDoPasso(Situacao.RECUSADO, null, motivo)
    }
}
