package com.clau.service_track.catalogo.infrastructure.adapter.config.mongodb

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "servicetrack.mongodb")
data class MongoCredentialProperties(

    val username: String = "",

    val password: String = "",

    val authDatabase: String = "admin",
) {

    val autenticado: Boolean
        get() = username.isNotBlank() && password.isNotBlank()
}
