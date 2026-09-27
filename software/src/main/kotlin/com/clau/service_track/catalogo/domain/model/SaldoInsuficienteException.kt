package com.clau.service_track.catalogo.domain.model

import com.clau.service_track.catalogo.domain.exception.DomainException
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import java.math.BigDecimal

class SaldoInsuficienteException(
    val insumoId: DomainId,
    val solicitado: BigDecimal,
    val disponivel: BigDecimal,
    unidade: UnidadeDeMedida,
) : DomainException(
    "Saldo insuficiente do insumo ${insumoId.value}: solicitado " +
        "${solicitado.stripTrailingZeros().toPlainString()} ${unidade.simbolo}, " +
        "disponível ${disponivel.stripTrailingZeros().toPlainString()} ${unidade.simbolo}"
)
