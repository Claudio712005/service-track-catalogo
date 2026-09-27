package com.clau.service_track.catalogo.application.port.`in`.useCase.insumo

import com.clau.service_track.catalogo.domain.model.Insumo
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import com.clau.service_track.catalogo.domain.vo.ValorMonetario

fun interface CriarInsumoUseCase {
    fun executar(comando: CriarInsumoCommand): Insumo
}

data class CriarInsumoCommand(
    val categoriaId: DomainId,
    val sku: String,
    val nome: String,
    val descricao: String,
    val custo: ValorMonetario,
    val unidadeDeMedida: UnidadeDeMedida?,
    val especificacao: Map<String, String>,
    val marca: String?,
    val fabricante: String?,
    val codigoFabricante: String?,
    val codigoBarras: String?,
    val controlaLote: Boolean,
    val validadeEmDias: Int?,
)
