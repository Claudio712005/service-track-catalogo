package com.clau.service_track.catalogo.infrastructure.adapter.config.mongodb

import com.mongodb.MongoCredential
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MongoCredentialProperties::class)
class MongoConfig {

    private val log = LoggerFactory.getLogger(MongoConfig::class.java)

    @Bean
    fun mongoClientSettingsBuilderCustomizer(
        propriedades: MongoCredentialProperties,
    ): MongoClientSettingsBuilderCustomizer = MongoClientSettingsBuilderCustomizer { construtor ->
        if (!propriedades.autenticado) {
            log.info("mongodb sem credencial: MONGODB_USERNAME ou MONGODB_PASSWORD ausente")
            return@MongoClientSettingsBuilderCustomizer
        }

        construtor.credential(
            MongoCredential.createCredential(
                propriedades.username,
                propriedades.authDatabase,
                propriedades.password.toCharArray(),
            )
        )
        log.info(
            "mongodb autenticado usuario={} authDatabase={}",
            propriedades.username,
            propriedades.authDatabase,
        )
    }
}
