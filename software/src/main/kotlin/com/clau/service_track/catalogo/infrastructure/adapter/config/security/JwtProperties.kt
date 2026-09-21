package com.clau.service_track.catalogo.infrastructure.adapter.config.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "servicetrack.security.jwt")
data class JwtProperties(

    val habilitado: Boolean = true,

    val issuer: String = "service-track-api",

    val chavePublica: String = "classpath:publicKey.pem",

    val claimDeRoles: String = "groups",
)
