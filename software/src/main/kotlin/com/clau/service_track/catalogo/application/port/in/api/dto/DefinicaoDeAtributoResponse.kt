package com.clau.service_track.catalogo.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "DefinicaoDeAtributoResponse",
    description = "Atributo declarado pela categoria."
)
data class DefinicaoDeAtributoResponse(

    @get:Schema(description = "Chave usada no mapa especificacao do insumo.", example = "viscosidade")
    val chave: String,

    @get:Schema(description = "Nome exibido ao operador.", example = "Viscosidade")
    val rotulo: String,

    @get:Schema(description = "Tipo do valor aceito.", example = "OPCAO")
    val tipo: String,

    @get:Schema(description = "Unidade exibida junto do valor.", example = "mm", nullable = true)
    val unidade: String?,

    @get:Schema(description = "Se o cadastro de insumo novo exige este atributo.", example = "true")
    val obrigatorio: Boolean,

    @get:Schema(description = "Valores aceitos quando o tipo é OPCAO.", example = "[\"0W20\", \"5W30\"]")
    val opcoes: List<String>,
)
