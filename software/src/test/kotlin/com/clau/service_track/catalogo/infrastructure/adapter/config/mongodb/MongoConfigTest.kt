package com.clau.service_track.catalogo.infrastructure.adapter.config.mongodb

import com.mongodb.MongoClientSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MongoConfigTest {

    private fun credencialResultante(propriedades: MongoCredentialProperties) =
        MongoClientSettings.builder()
            .also { construtor ->
                MongoConfig().mongoClientSettingsBuilderCustomizer(propriedades).customize(construtor)
            }
            .build()
            .credential

    @Test
    fun `usuario e senha presentes viram credencial no authDatabase do Atlas`() {
        val credencial = assertNotNull(
            credencialResultante(
                MongoCredentialProperties(username = "st_ins_app", password = "senha-do-atlas")
            )
        )

        assertEquals("st_ins_app", credencial.userName)
        assertEquals("admin", credencial.source, "o usuario do Atlas e criado no banco admin")
        assertEquals("senha-do-atlas", credencial.password?.concatToString())
    }

    @Test
    fun `sem credencial nenhuma o cliente fica sem autenticacao, e nao com usuario vazio`() {
        assertNull(
            credencialResultante(MongoCredentialProperties()),
            "credencial vazia faria o driver tentar autenticar com usuario em branco e falhar " +
                "na conexao; o compose local e o smoke da esteira sobem sem usuario",
        )
    }

    @Test
    fun `senha ausente nao autentica, mesmo com usuario definido`() {
        assertNull(credencialResultante(MongoCredentialProperties(username = "st_ins_app")))
        assertNull(credencialResultante(MongoCredentialProperties(password = "so-a-senha")))
    }

    @Test
    fun `authDatabase e configuravel, para o caso de o usuario nao viver em admin`() {
        val credencial = assertNotNull(
            credencialResultante(
                MongoCredentialProperties(
                    username = "u",
                    password = "p",
                    authDatabase = "ST_INS",
                )
            )
        )

        assertEquals("ST_INS", credencial.source)
    }
}
