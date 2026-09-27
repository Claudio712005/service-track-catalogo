package com.clau.service_track.catalogo.infrastructure.entity.mongo

import com.clau.service_track.catalogo.domain.vo.UnidadeDeMedida
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal
import java.time.Instant

@Document(collection = "INSUMOS")
class InsumoDocumento(
    @Id
    var id: String,

    @Field("CATEGORIA_ID")
    var categoriaId: String,

    @Field("SKU")
    var sku: String,

    @Field("NOME")
    var nome: String,

    @Field("DESCRICAO")
    var descricao: String = "",

    @Field("MARCA")
    var marca: String? = null,

    @Field("FABRICANTE")
    var fabricante: String? = null,

    @Field("CODIGO_FABRICANTE")
    var codigoFabricante: String? = null,

    @Field("CODIGO_BARRAS")
    var codigoBarras: String? = null,

    @Field("UNIDADE_MEDIDA")
    var unidadeMedida: UnidadeDeMedida,

    @Field(name = "CUSTO_PADRAO", targetType = FieldType.DECIMAL128)
    var custoPadrao: BigDecimal,

    @Field("CONTROLA_LOTE")
    var controlaLote: Boolean = false,

    @Field("VALIDADE_EM_DIAS")
    var validadeEmDias: Int? = null,

    @Field("ESPECIFICACAO")
    var especificacao: MutableMap<String, String> = mutableMapOf(),

    @Field("ATIVO")
    var ativo: Boolean = true,

    @Version
    @Field("VERSAO")
    var versao: Long? = null,

    @Field("DATA_CRIACAO")
    var dataCriacao: Instant,

    @Field("DATA_ATUALIZACAO")
    var dataAtualizacao: Instant,
)
