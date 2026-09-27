package com.clau.service_track.catalogo.infrastructure.entity.mongo

import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import org.springframework.data.mongodb.core.mapping.Field

class DefinicaoDeAtributoDocumento(
    @Field("CHAVE")
    var chave: String,

    @Field("ROTULO")
    var rotulo: String,

    @Field("TIPO")
    var tipo: TipoDeAtributo,

    @Field("UNIDADE")
    var unidade: String? = null,

    @Field("OBRIGATORIO")
    var obrigatorio: Boolean,

    @Field("OPCOES")
    var opcoes: MutableList<String> = mutableListOf(),

    @Field("ORDEM")
    var ordem: Int = 0,
)
