package com.clau.service_track.catalogo.infrastructure.adapter.out.mensageria

import com.networknt.schema.InputFormat
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion

object EsquemasDeEstoque {

    const val COMANDOS = "/contratos/comandos-de-estoque-v1.json"
    const val EVENTOS = "/contratos/eventos-de-estoque-v1.json"

    fun carregar(recurso: String): JsonSchema {
        val conteudo = requireNotNull(EsquemasDeEstoque::class.java.getResourceAsStream(recurso)) {
            "Esquema $recurso nao esta no classpath de teste: confira a copia de docs/mensageria/esquemas em processTestResources"
        }

        val configuracao = SchemaValidatorsConfig.builder()
            .formatAssertionsEnabled(true)
            .build()

        return JsonSchemaFactory
            .getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(conteudo, configuracao)
    }

    fun violacoes(esquema: JsonSchema, json: String): List<String> =
        esquema.validate(json, InputFormat.JSON).map { it.message }
}
