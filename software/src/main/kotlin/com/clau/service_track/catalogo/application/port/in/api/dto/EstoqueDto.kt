package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(
    name = "EntradaDeEstoqueRequest",
    description = "Entrada de estoque: o que chegou na prateleira. É o único movimento que a API " +
        "aceita por HTTP — reserva, consumo e liberação nascem da saga, por mensagem no Kafka, " +
        "porque pertencem ao ciclo da ordem de serviço e não a uma ação isolada de operador."
)
data class EntradaDeEstoqueRequest(

    @field:NotNull(message = "Quantidade é obrigatória")
    @field:DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
    @field:Digits(integer = 10, fraction = 4, message = "Quantidade admite até 10 inteiros e 4 decimais")
    @get:Schema(
        description = "Quantidade que entrou, na unidade do insumo. Unidade não fracionável " +
            "(UNIDADE, PECA, CONJUNTO) exige valor inteiro.",
        example = "24",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val quantidade: BigDecimal,

    @field:DecimalMin(value = "0.00", message = "Custo unitário não pode ser negativo")
    @field:Digits(integer = 10, fraction = 2, message = "Custo unitário admite até 2 decimais")
    @get:Schema(
        description = "Custo de aquisição por unidade, quando conhecido. Fica no movimento, para " +
            "a auditoria responder quanto custou o que foi consumido.",
        example = "38.90",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val custoUnitario: BigDecimal? = null,

    @get:Schema(
        description = "Por que o estoque aumentou. NOTA_ENTRADA para compra recebida, " +
            "INVENTARIO para contagem física, AJUSTE_MANUAL para correção deliberada.",
        example = "NOTA_ENTRADA",
        allowableValues = ["NOTA_ENTRADA", "INVENTARIO", "AJUSTE_MANUAL"],
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val origemTipo: String = "NOTA_ENTRADA",

    @get:Schema(
        description = "Identificador do documento de origem — a nota fiscal, o inventário. " +
            "Guardado no movimento para a conferência posterior.",
        example = "018f3d21-77aa-7c10-9d31-4b2c8e0f1a55",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val origemId: String? = null,
)

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

@Schema(
    name = "ReservaDeEstoqueResponse",
    description = "Reserva ativa de um insumo para uma ordem de serviço."
)
data class ReservaDeEstoqueResponse(

    @get:Schema(description = "Identificador da reserva.", example = "018f4a02-31bc-7d55-9f21-7a0c4e2b6d10")
    val reservaId: String,

    @get:Schema(
        description = "Ordem de serviço que detém a reserva.",
        example = "018f4a01-1120-7e33-8a10-5c9b3d7f2e40"
    )
    val ordemServicoId: String,

    @get:Schema(description = "Quantidade reservada.", example = "6.0000")
    val quantidade: BigDecimal,

    @get:Schema(
        description = "Quando a reserva vence, em UTC. Vencida e não resolvida pela saga, é " +
            "devolvida ao disponível pela rotina de expiração. Nulo significa sem prazo.",
        example = "2026-09-27T18:00:00"
    )
    val expiraEm: String?,
)
