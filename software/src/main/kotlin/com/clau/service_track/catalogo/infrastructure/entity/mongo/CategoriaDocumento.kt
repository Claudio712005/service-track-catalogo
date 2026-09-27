package com.clau.service_track.catalogo.infrastructure.entity.mongo

import com.clau.service_track.catalogo.domain.vo.TipoDeAtributo
import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

@Document(collection = "CATEGORIAS")
class CategoriaDocumento(
    @Id
    var id: String,

    @Field("CODIGO")
    var codigo: String,

    @Field("NOME")
    var nome: String,

    @Field("UNIDADE_PADRAO")
    var unidadePadrao: UnidadeDeMedida,

    @Field("ATIVA")
    var ativa: Boolean = true,

    @Field("ATRIBUTOS")
    var atributos: MutableList<DefinicaoDeAtributoDocumento> = mutableListOf(),

    @Field("DATA_CRIACAO")
    var dataCriacao: Instant,

    @Field("DATA_ATUALIZACAO")
    var dataAtualizacao: Instant,
)

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
