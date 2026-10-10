package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria.dto

data class DadosDeConsumoRecusado(
    val insumoId: String,
    val sku: String,
    val ordemServicoId: String,
    val motivo: String,
)
