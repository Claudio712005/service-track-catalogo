package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto

import java.math.BigDecimal

data class DadosDeReservaRecusada(
    val insumoId: String,
    val sku: String,
    val ordemServicoId: String,
    val quantidadeSolicitada: BigDecimal,
    val unidadeDeMedida: String,
    val motivo: String,
)
