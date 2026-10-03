package com.clau.service_track.catalogo.infrastructure.adapter.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class CorrelacaoFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(CorrelacaoFilter::class.java)

    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun doFilterInternal(
        requisicao: HttpServletRequest,
        resposta: HttpServletResponse,
        cadeia: FilterChain,
    ) {
        val correlacao = sanear(requisicao.getHeader(CABECALHO_CORRELACAO)) ?: gerar()
        val requisicaoId = gerar()

        MDC.put(CHAVE_CORRELACAO, correlacao)
        MDC.put(CHAVE_REQUISICAO, requisicaoId)
        resposta.setHeader(CABECALHO_CORRELACAO, correlacao)
        resposta.setHeader(CABECALHO_REQUISICAO, requisicaoId)

        val inicio = System.nanoTime()
        try {
            cadeia.doFilter(requisicao, resposta)
        } finally {
            registrar(requisicao, resposta, inicio)
            MDC.remove(CHAVE_CORRELACAO)
            MDC.remove(CHAVE_REQUISICAO)
        }
    }

    private fun registrar(requisicao: HttpServletRequest, resposta: HttpServletResponse, inicio: Long) {
        if (ignorado(requisicao.requestURI)) return

        val duracao = (System.nanoTime() - inicio) / 1_000_000
        val metodo = requisicao.method
        val rota = rotaDe(requisicao)
        val status = resposta.status

        when {
            status >= 500 -> log.error("requisicao concluida metodo={} rota={} status={} duracaoMs={}", metodo, rota, status, duracao)
            status >= 400 -> log.warn("requisicao concluida metodo={} rota={} status={} duracaoMs={}", metodo, rota, status, duracao)
            metodo != "GET" -> log.info("requisicao concluida metodo={} rota={} status={} duracaoMs={}", metodo, rota, status, duracao)
            else -> log.debug("requisicao concluida metodo={} rota={} status={} duracaoMs={}", metodo, rota, status, duracao)
        }
    }

    private fun ignorado(rota: String): Boolean = ROTAS_IGNORADAS.any { rota.startsWith(it) }

    private fun sanear(bruto: String?): String? = bruto
        ?.trim()
        ?.take(TAMANHO_MAXIMO)
        ?.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        ?.ifBlank { null }

    private fun gerar(): String = UUID.randomUUID().toString()

    companion object {

        fun rotaDe(requisicao: HttpServletRequest): String =
            requisicao.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
                ?: requisicao.requestURI

        const val CABECALHO_CORRELACAO = "X-Correlation-Id"
        const val CABECALHO_REQUISICAO = "X-Request-Id"
        const val CHAVE_CORRELACAO = "correlationId"
        const val CHAVE_REQUISICAO = "requestId"
        private const val TAMANHO_MAXIMO = 64
        private val ROTAS_IGNORADAS = listOf("/actuator", "/v3/api-docs", "/swagger-ui")
    }
}
