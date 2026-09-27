package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario

fun interface AtualizarInsumoUseCase {
    fun executar(comando: AtualizarInsumoCommand): Insumo
}

data class AtualizarInsumoCommand(
    val id: DomainId,
    val descricao: String,
    val custo: ValorMonetario,
    val especificacao: Map<String, String>,
    val marca: String?,
    val fabricante: String?,
    val codigoFabricante: String?,
    val codigoBarras: String?,
    val controlaLote: Boolean,
    val validadeEmDias: Int?,
)
