package com.clau.service_track.catalogo.infrastructure.entity.mongo

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
