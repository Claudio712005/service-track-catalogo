package com.clau.service_track.catalogo.infrastructure.adapter.web.error

import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class FabricaDeErro {

    fun montar(
        status: HttpStatus,
        codigo: CodigoErro,
        mensagem: String,
        requisicao: HttpServletRequest,
        violacoes: List<ErrorResponse.Violacao>? = null,
    ): ResponseEntity<ErrorResponse> = ResponseEntity.status(status).body(
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            code = codigo,
            message = mensagem,
            path = requisicao.requestURI,
            traceId = traceIdAtual(),
            violacoes = violacoes,
        )
    )

    fun traceIdAtual(): String? = Span.current().spanContext
        .takeIf { it.isValid }
        ?.traceId
}
