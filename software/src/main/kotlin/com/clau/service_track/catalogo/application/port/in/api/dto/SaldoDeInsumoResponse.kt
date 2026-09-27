package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(
    name = "SaldoDeInsumoResponse",
    description = "Saldo de um insumo. Disponível é o que pode ser prometido; reservado é o que já " +
        "foi prometido a uma ordem de serviço e ainda não saiu. Os dois são separados de propósito: " +
        "sem essa separação, duas ordens prometem a mesma peça."
)
data class SaldoDeInsumoResponse(

    @get:Schema(description = "Identificador do insumo.", example = "018f3c10-9a12-7b44-8e01-2c5d7f8a9b31")
    val insumoId: String,

    @get:Schema(description = "SKU do insumo, em maiúsculas.", example = "OL-5W30-SN-1L")
    val sku: String,

    @get:Schema(description = "Nome do insumo.", example = "Óleo 5W30 sintético 1L")
    val nome: String,

    @get:Schema(description = "Unidade em que as quantidades estão expressas.", example = "LITRO")
    val unidadeDeMedida: String,

    @get:Schema(description = "Quantidade livre para novas reservas.", example = "18.0000")
    val quantidadeDisponivel: BigDecimal,

    @get:Schema(description = "Quantidade comprometida com reservas ativas.", example = "6.0000")
    val quantidadeReservada: BigDecimal,

    @get:Schema(
        description = "Piso de reposição. Disponível abaixo dele gera alerta em log, não bloqueio.",
        example = "10.0000"
    )
    val estoqueMinimo: BigDecimal,

    @get:Schema(description = "Se o disponível está abaixo do estoque mínimo.", example = "false")
    val abaixoDoMinimo: Boolean,

    @get:Schema(
        description = "Momento da última alteração do saldo, em UTC.",
        example = "2026-09-27T12:41:07.882"
    )
    val dataAtualizacao: String,

    @get:Schema(description = "Reservas ativas que compõem o total reservado.")
    val reservas: List<ReservaDeEstoqueResponse>,
)
