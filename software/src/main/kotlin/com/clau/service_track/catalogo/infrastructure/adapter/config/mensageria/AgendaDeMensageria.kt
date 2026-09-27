package com.clau.service_track.catalogo.infrastructure.adapter.mensageria

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "servicetrack.mensageria", name = ["habilitada"], havingValue = "true")
class AgendaDeMensageria(
    private val propriedades: PropriedadesDeMensageria,
    private val publicador: PublicadorDeOutbox,
    private val expiracao: ObjectProvider<RotinaDeExpiracaoDeReservas>,
) : SchedulingConfigurer {

    private val log = LoggerFactory.getLogger(AgendaDeMensageria::class.java)

    private val falhasSeguidas = ConcurrentHashMap<String, AtomicInteger>()

    override fun configureTasks(registrar: ScheduledTaskRegistrar) {
        registrar.addFixedDelayTask(
            { executar(PUBLICACAO) { publicador.publicarPendentes() } },
            propriedades.intervaloDePublicacao,
        )

        expiracao.ifAvailable { rotina ->
            registrar.addFixedDelayTask(
                { executar(EXPIRACAO) { rotina.expirarVencidas() } },
                propriedades.expiracaoDeReservas.intervalo,
            )
        }
    }

    private fun executar(tarefa: String, bloco: () -> Int) {
        try {
            bloco()
            anunciarRecuperacao(tarefa)
        } catch (e: Exception) {
            registrarFalha(tarefa, e)
        }
    }

    private fun registrarFalha(tarefa: String, e: Exception) {
        val seguidas = falhasSeguidas.computeIfAbsent(tarefa) { AtomicInteger() }.incrementAndGet()
        val causa = e.cause?.javaClass?.simpleName ?: e.javaClass.simpleName

        when {
            seguidas == 1 -> log.error("tarefa periodica falhou tarefa={} causa={}", tarefa, causa, e)
            seguidas % FALHAS_ENTRE_AVISOS == 0 ->
                log.warn("tarefa periodica ainda falhando tarefa={} tentativas={} causa={}", tarefa, seguidas, causa)
        }
    }

    private fun anunciarRecuperacao(tarefa: String) {
        val seguidas = falhasSeguidas.remove(tarefa)?.get() ?: return
        log.info("tarefa periodica voltou a funcionar tarefa={} falhas anteriores={}", tarefa, seguidas)
    }

    private companion object {
        const val PUBLICACAO = "publicacao do outbox"
        const val EXPIRACAO = "expiracao de reservas"
        const val FALHAS_ENTRE_AVISOS = 30
    }
}
