package com.clau.service_track.catalogo.infrastructure.adapter.config

import com.clau.service_track.catalogo.infrastructure.adapter.web.filter.CorrelacaoFilter
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(value = ["springdoc.swagger-ui.enabled"], havingValue = "true", matchIfMissing = true)
class OpenApiConfig(
    @param:Value("\${servicetrack.security.jwt.habilitado:true}")
    private val autenticacaoHabilitada: Boolean,
) {

    @Bean
    fun openApi(): OpenAPI {
        val api = OpenAPI().info(
            Info()
                .title("ServiceTrack — Catálogo")
                .version("1")
                .description(
                    "Catálogo de serviços e de insumos da oficina. Fonte canônica de nome, " +
                        "valor de referência, características técnicas e custo de tabela para " +
                        "composição de orçamentos pelo serviço de ordens de serviço. " +
                        "Saldo de estoque não é publicado por esta versão." +
                        if (autenticacaoHabilitada) "" else
                            "\n\nATENÇÃO: esta instância está com a validação de token desligada. " +
                                "Configuração exclusiva de desenvolvimento local."
                )
        )

        if (autenticacaoHabilitada) {
            api.components(
                Components().addSecuritySchemes(
                    ESQUEMA_BEARER,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Token RS256 emitido pela função de autenticação. Obtenha em " +
                                "POST /autenticacao com CPF e senha, e envie como " +
                                "Authorization: Bearer <token>. As permissões vêm do claim groups."
                        )
                )
            )
        }
        return api
    }

    @Bean
    fun exigirTokenNasOperacoes(): OperationCustomizer = OperationCustomizer { operacao, _ ->
        if (autenticacaoHabilitada) {
            operacao.addSecurityItem(SecurityRequirement().addList(ESQUEMA_BEARER))
        }
        operacao
    }

    @Bean
    fun documentarCorrelacao(): OperationCustomizer = OperationCustomizer { operacao, _ ->
        operacao.addParametersItem(
            Parameter()
                .`in`("header")
                .name(CorrelacaoFilter.CABECALHO_CORRELACAO)
                .required(false)
                .schema(StringSchema())
                .description(
                    "Identificador de correlação da operação de negócio, propagado entre serviços. " +
                        "Informe o mesmo valor em todas as chamadas do fluxo para que o rastreamento " +
                        "as agrupe; omitido, o servidor gera um. É devolvido no cabeçalho de resposta " +
                        "de mesmo nome, ao lado de " + CorrelacaoFilter.CABECALHO_TRANSACAO +
                        ", que identifica esta requisição isolada."
                )
        )
        operacao
    }

    companion object {
        private const val ESQUEMA_BEARER = "bearerAuth"
    }
}
