package com.clau.service_track.catalogo.infrastructure.adapter.config

import io.swagger.v3.oas.models.Components
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
                    "Catálogo de serviços e gestão de estoque de insumos da oficina. " +
                        "Fonte canônica de nome, valor de referência e saldo disponível para " +
                        "composição de orçamentos pelo serviço de ordens de serviço." +
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

    companion object {
        private const val ESQUEMA_BEARER = "bearerAuth"
    }
}
