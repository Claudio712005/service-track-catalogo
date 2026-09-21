package com.clau.service_track.catalogo.infrastructure.adapter.config.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.HandlerExceptionResolver
import java.security.interfaces.RSAPublicKey

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
@ConditionalOnProperty(
    prefix = "servicetrack.security.jwt",
    name = ["habilitado"],
    havingValue = "true",
    matchIfMissing = true,
)
class SecurityConfig(
    private val propriedades: JwtProperties,
    private val resourceLoader: ResourceLoader,
) {

    @Bean
    fun filterChain(
        http: HttpSecurity,
        @Qualifier("handlerExceptionResolver") resolvedorDeExcecao: HandlerExceptionResolver,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers(*CAMINHOS_PUBLICOS).permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt -> jwt.jwtAuthenticationConverter(conversorDeAutoridades()) }
                oauth2.authenticationEntryPoint { requisicao, resposta, excecao ->
                    resolvedorDeExcecao.resolveException(requisicao, resposta, null, excecao)
                }
                oauth2.accessDeniedHandler { requisicao, resposta, excecao ->
                    resolvedorDeExcecao.resolveException(requisicao, resposta, null, excecao)
                }
            }
        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val recurso = resourceLoader.getResource(propriedades.chavePublica)
        require(recurso.exists()) {
            "Chave pública JWT não encontrada em '${propriedades.chavePublica}'. " +
                "Defina servicetrack.security.jwt.chave-publica apontando para o PEM emitido junto com a Lambda."
        }

        val chave: RSAPublicKey = recurso.inputStream.use { RsaKeyConverters.x509().convert(it) }
            ?: error("Conteúdo de '${propriedades.chavePublica}' não é uma chave pública X.509 válida")

        val decodificador = NimbusJwtDecoder.withPublicKey(chave).build()
        decodificador.setJwtValidator(validadores())
        return decodificador
    }

    private fun validadores(): OAuth2TokenValidator<Jwt> =
        JwtValidators.createDefaultWithIssuer(propriedades.issuer)

    private fun conversorDeAutoridades(): JwtAuthenticationConverter {
        val conversor = JwtAuthenticationConverter()
        conversor.setJwtGrantedAuthoritiesConverter { token ->
            token.getClaimAsStringList(propriedades.claimDeRoles)
                .orEmpty()
                .map { SimpleGrantedAuthority("$PREFIXO_ROLE$it") }
        }
        return conversor
    }

    companion object {
        private const val PREFIXO_ROLE = "ROLE_"

        private val CAMINHOS_PUBLICOS = arrayOf(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
        )
    }
}
