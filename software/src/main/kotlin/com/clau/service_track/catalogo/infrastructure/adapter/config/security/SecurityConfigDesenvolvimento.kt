package com.clau.service_track.catalogo.infrastructure.adapter.config.security

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import jakarta.annotation.PostConstruct

@Configuration
@ConditionalOnProperty(
    prefix = "servicetrack.security.jwt",
    name = ["habilitado"],
    havingValue = "false",
)
class SecurityConfigDesenvolvimento {

    private val log = LoggerFactory.getLogger(SecurityConfigDesenvolvimento::class.java)

    @PostConstruct
    fun avisar() {
        log.warn(
            "Validação de JWT DESLIGADA (servicetrack.security.jwt.habilitado=false). " +
                "Todas as rotas estão abertas. Esta configuração só deve existir em desenvolvimento local."
        )
    }

    @Bean
    fun filterChainSemAutenticacao(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }
}
