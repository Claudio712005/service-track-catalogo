package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(
    name = "DefinicaoDeAtributoRequest",
    description = "Declaração de um atributo que os insumos da categoria passam a aceitar. " +
        "É este cadastro que permite acrescentar característica nova sem alterar o sistema."
)
data class DefinicaoDeAtributoRequest(

    @field:NotBlank(message = "Chave do atributo não pode ser vazia")
    @field:Pattern(
        regexp = "^[a-z][a-z0-9-]*$",
        message = "Chave admite apenas letras minúsculas, dígitos e hífen, começando por letra"
    )
    @field:Size(max = 60, message = "Chave excede 60 caracteres")
    @get:Schema(
        description = "Identificador do atributo dentro da categoria. É a chave usada no mapa " +
            "especificacao do insumo, e não muda depois de existir insumo que a use.",
        example = "viscosidade",
        pattern = "^[a-z][a-z0-9-]*$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val chave: String,

    @field:NotBlank(message = "Rótulo do atributo não pode ser vazio")
    @field:Size(max = 120, message = "Rótulo excede 120 caracteres")
    @get:Schema(
        description = "Nome exibido ao operador na tela de cadastro do insumo.",
        example = "Viscosidade",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val rotulo: String,

    @get:Schema(
        description = "Tipo do valor aceito. TEXTO aceita qualquer texto; INTEIRO e DECIMAL " +
            "recusam valor não numérico; BOOLEANO aceita true, false, sim, nao, 1 e 0; " +
            "OPCAO restringe aos valores listados em opcoes.",
        example = "OPCAO",
        allowableValues = ["TEXTO", "INTEIRO", "DECIMAL", "BOOLEANO", "OPCAO"],
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val tipo: String,

    @get:Schema(
        description = "Unidade exibida junto do valor, quando faz sentido. Não participa de cálculo.",
        example = "mm",
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val unidade: String? = null,

    @get:Schema(
        description = "Atributo obrigatório passa a ser exigido no cadastro de todo insumo novo " +
            "da categoria. Ao acrescentar atributo numa categoria que já tem insumos, ele nasce " +
            "opcional por imposição do domínio: o acervo existente não o possui.",
        example = "true",
        defaultValue = "false"
    )
    val obrigatorio: Boolean = false,

    @get:Schema(
        description = "Valores aceitos. Obrigatório quando tipo é OPCAO, e proibido nos demais tipos. " +
            "A comparação ignora caixa.",
        example = "[\"0W20\", \"5W30\", \"10W40\", \"15W40\"]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val opcoes: List<String> = emptyList(),
)
