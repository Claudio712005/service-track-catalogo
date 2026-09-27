package com.clau.service_track.catalogo.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.catalogo.application.port.out.repository.ServicoRepositoryPort
import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("integracao")
class ServicoRepositoryPostgresAdapterIT {

    @Autowired
    private lateinit var repositorio: ServicoRepositoryPort

    @Test
    fun `grava e reconstitui o servico com valor e datas preservados`() {
        val servico = Servico.criar(
            nome = "Troca de pastilhas dianteiras",
            descricao = "Substituição do par de pastilhas dianteiras com limpeza do conjunto.",
            valorReferencia = ValorMonetario.de("289.90"),
        )

        repositorio.salvar(servico)
        val recuperado = repositorio.buscarPorId(servico.id)

        assertNotNull(recuperado)
        assertEquals(servico.nome, recuperado.nome)
        assertEquals(servico.descricao, recuperado.descricao)
        assertEquals(ValorMonetario.de("289.90"), recuperado.valorReferencia)
        assertEquals(servico.dataCriacao.withNano(0), recuperado.dataCriacao.withNano(0))
        assertTrue(recuperado.ativo)
    }

    @Test
    fun `atualizacao altera descricao e valor sem duplicar registro`() {
        val servico = Servico.criar(
            nome = "Sangria do sistema de freio",
            descricao = "Troca do fluido e sangria das quatro rodas.",
            valorReferencia = ValorMonetario.de("160.00"),
        )
        repositorio.salvar(servico)

        servico.atualizarDescricao("Troca do fluido, sangria das quatro rodas e teste de pedal.")
        servico.atualizarValorReferencia(ValorMonetario.de("199.90"))
        repositorio.salvar(servico)

        val recuperado = repositorio.buscarPorId(servico.id)

        assertNotNull(recuperado)
        assertEquals("Troca do fluido, sangria das quatro rodas e teste de pedal.", recuperado.descricao)
        assertEquals(ValorMonetario.de("199.90"), recuperado.valorReferencia)
        assertEquals(1, repositorio.listar(incluirInativos = true).count { it.id == servico.id })
    }

    @Test
    fun `desativado sai da listagem padrao e libera o nome`() {
        val servico = Servico.criar(
            nome = "Recarga de gás do ar-condicionado",
            descricao = "Recarga com verificação de vazamento.",
            valorReferencia = ValorMonetario.de("240.00"),
        )
        repositorio.salvar(servico)

        assertTrue(repositorio.existeComNome("recarga de gás do ar-condicionado"))

        servico.desativar()
        repositorio.salvar(servico)

        assertTrue(repositorio.listar(incluirInativos = false).none { it.id == servico.id })
        assertTrue(repositorio.listar(incluirInativos = true).any { it.id == servico.id })
        assertFalse(repositorio.existeComNome("Recarga de gás do ar-condicionado"))
    }

    @Test
    fun `identificador inexistente devolve nulo`() {
        assertNull(repositorio.buscarPorId(DomainId.gerar()))
    }

    companion object {

        private val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("st_cat")
            .withUsername("st_cat_user")
            .withPassword("st_cat_teste")
            .withInitScript("db/01_baseline_st_cat.sql")

        @JvmStatic
        @BeforeAll
        fun iniciar() {
            assumeTrue(
                DockerClientFactory.instance().isDockerAvailable,
                "Docker indisponivel: teste de integracao ignorado",
            )
            if (!postgres.isRunning) {
                postgres.start()
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun propriedades(registro: DynamicPropertyRegistry) {
            registro.add("spring.datasource.url") { postgres.jdbcUrl }
            registro.add("spring.datasource.username") { postgres.username }
            registro.add("spring.datasource.password") { postgres.password }
            registro.add("spring.mongodb.uri") { "mongodb://localhost:27017/ST_INS" }
        }
    }
}
