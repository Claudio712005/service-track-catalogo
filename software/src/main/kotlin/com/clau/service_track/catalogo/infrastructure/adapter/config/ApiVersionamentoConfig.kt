package com.clau.service_track.catalogo.infrastructure.adapter.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiVersionamentoConfig : WebMvcConfigurer {

    override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
        configurer
            .useRequestHeader(CABECALHO_DE_VERSAO)
            .addSupportedVersions(VERSAO_ATUAL)
            .setDefaultVersion(VERSAO_ATUAL)
            .setVersionRequired(false)
    }

    companion object {
        const val CABECALHO_DE_VERSAO = "X-API-Version"
        const val VERSAO_ATUAL = "1"
    }
}
