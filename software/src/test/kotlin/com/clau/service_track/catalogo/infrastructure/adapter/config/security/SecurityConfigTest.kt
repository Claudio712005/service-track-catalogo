package com.clau.service_track.catalogo.infrastructure.adapter.config.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.oauth2.jwt.JwtException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SecurityConfigTest {

    private val par: KeyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

    private val propriedades = JwtProperties(
        habilitado = true,
        issuer = ISSUER,
        chavePublica = "memoria:publicKey.pem",
        claimDeRoles = "groups",
    )

    private val decodificador = SecurityConfig(propriedades, carregadorEmMemoria()).jwtDecoder()

    @Test
    fun `aceita token assinado pela chave correspondente`() {
        val token = decodificador.decode(assinar(issuer = ISSUER))

        assertEquals(SUBJECT, token.subject)
        assertEquals(listOf("MECANICO", "CLIENTE"), token.getClaimAsStringList("groups"))
        assertEquals("13646633093", token.getClaimAsString("cpf"))
    }

    @Test
    fun `recusa token de outro emissor`() {
        assertFailsWith<JwtException> { decodificador.decode(assinar(issuer = "emissor-desconhecido")) }
    }

    @Test
    fun `recusa token expirado`() {
        val expirado = assinar(issuer = ISSUER, expiraEm = Instant.now().minusSeconds(60))
        assertFailsWith<JwtException> { decodificador.decode(expirado) }
    }

    @Test
    fun `recusa token assinado por outra chave`() {
        val intruso = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val token = assinar(issuer = ISSUER, chave = intruso.private as RSAPrivateKey)
        assertFailsWith<JwtException> { decodificador.decode(token) }
    }

    private fun assinar(
        issuer: String,
        expiraEm: Instant = Instant.now().plusSeconds(3600),
        chave: RSAPrivateKey = par.private as RSAPrivateKey,
    ): String {
        val claims = JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(SUBJECT)
            .claim("upn", "joao.silva@example.com")
            .claim("groups", listOf("MECANICO", "CLIENTE"))
            .claim("cpf", "13646633093")
            .issueTime(Date.from(Instant.now().minusSeconds(5)))
            .expirationTime(Date.from(expiraEm))
            .build()

        return SignedJWT(JWSHeader(JWSAlgorithm.RS256), claims)
            .apply { sign(RSASSASigner(chave)) }
            .serialize()
    }

    private fun carregadorEmMemoria(): ResourceLoader = object : ResourceLoader {
        override fun getResource(location: String): Resource = ByteArrayResource(pemDaChavePublica())
        override fun getClassLoader(): ClassLoader = this::class.java.classLoader
    }

    private fun pemDaChavePublica(): ByteArray {
        val base64 = Base64.getMimeEncoder(64, "\n".toByteArray())
            .encodeToString((par.public as RSAPublicKey).encoded)
        return "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----\n".toByteArray()
    }

    companion object {
        private const val ISSUER = "service-track-api"
        private const val SUBJECT = "550e8400-e29b-41d4-a716-446655440001"
    }
}
