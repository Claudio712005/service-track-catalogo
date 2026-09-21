package com.clau.service_track.catalogo.infrastructure.adapter.web.error

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val fabrica: FabricaDeErro,
) {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun corpoInvalido(
        e: MethodArgumentNotValidException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val violacoes = e.bindingResult.fieldErrors.map {
            ErrorResponse.Violacao(
                campo = it.field,
                mensagem = it.defaultMessage ?: "Valor inválido",
                valorRejeitado = it.rejectedValue?.toString(),
            )
        }
        return fabrica.montar(
            status = HttpStatus.BAD_REQUEST,
            codigo = CodigoErro.REQUISICAO_INVALIDA,
            mensagem = "Corpo da requisição contém ${violacoes.size} campo(s) inválido(s)",
            requisicao = requisicao,
            violacoes = violacoes,
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun parametroInvalido(
        e: ConstraintViolationException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val violacoes = e.constraintViolations.map {
            ErrorResponse.Violacao(
                campo = it.propertyPath.toString().substringAfterLast('.'),
                mensagem = it.message,
                valorRejeitado = it.invalidValue?.toString(),
            )
        }
        return fabrica.montar(
            status = HttpStatus.BAD_REQUEST,
            codigo = CodigoErro.REQUISICAO_INVALIDA,
            mensagem = "Parâmetros da requisição violam ${violacoes.size} restrição(ões)",
            requisicao = requisicao,
            violacoes = violacoes,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun corpoIlegivel(
        e: HttpMessageNotReadableException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.BAD_REQUEST,
        codigo = CodigoErro.CORPO_ILEGIVEL,
        mensagem = "Corpo da requisição não é um JSON válido ou não corresponde ao schema esperado",
        requisicao = requisicao,
    )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun tipoIncompativel(
        e: MethodArgumentTypeMismatchException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.BAD_REQUEST,
        codigo = CodigoErro.PARAMETRO_INVALIDO,
        mensagem = "Parâmetro '${e.name}' não aceita o valor informado",
        requisicao = requisicao,
        violacoes = listOf(
            ErrorResponse.Violacao(
                campo = e.name,
                mensagem = "Tipo esperado: ${e.requiredType?.simpleName ?: "desconhecido"}",
                valorRejeitado = e.value?.toString(),
            )
        ),
    )

    @ExceptionHandler(ConflitoDeEstadoException::class)
    fun conflitoDeEstado(
        e: ConflitoDeEstadoException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.CONFLICT,
        codigo = CodigoErro.CONFLITO_DE_ESTADO,
        mensagem = e.message ?: CodigoErro.CONFLITO_DE_ESTADO.descricao,
        requisicao = requisicao,
    )

    @ExceptionHandler(DomainException::class)
    fun regraDeNegocio(
        e: DomainException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.BAD_REQUEST,
        codigo = CodigoErro.REGRA_DE_NEGOCIO,
        mensagem = e.message ?: CodigoErro.REGRA_DE_NEGOCIO.descricao,
        requisicao = requisicao,
    )

    @ExceptionHandler(RecursoNaoEncontradoException::class)
    fun naoEncontrado(
        e: RecursoNaoEncontradoException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.NOT_FOUND,
        codigo = CodigoErro.RECURSO_NAO_ENCONTRADO,
        mensagem = e.message ?: CodigoErro.RECURSO_NAO_ENCONTRADO.descricao,
        requisicao = requisicao,
    )

    @ExceptionHandler(NoResourceFoundException::class)
    fun rotaInexistente(
        e: NoResourceFoundException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.NOT_FOUND,
        codigo = CodigoErro.RECURSO_NAO_ENCONTRADO,
        mensagem = "Nenhum recurso mapeado para este caminho",
        requisicao = requisicao,
    )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodoNaoSuportado(
        e: HttpRequestMethodNotSupportedException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.METHOD_NOT_ALLOWED,
        codigo = CodigoErro.METODO_NAO_SUPORTADO,
        mensagem = "Método ${e.method} não é suportado neste recurso",
        requisicao = requisicao,
    )

    @ExceptionHandler(AuthenticationException::class)
    fun naoAutenticado(
        e: AuthenticationException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.UNAUTHORIZED,
        codigo = CodigoErro.NAO_AUTENTICADO,
        mensagem = "Token ausente, expirado, com assinatura inválida ou emissor não reconhecido",
        requisicao = requisicao,
    )

    @ExceptionHandler(AccessDeniedException::class)
    fun naoAutorizado(
        e: AccessDeniedException,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = fabrica.montar(
        status = HttpStatus.FORBIDDEN,
        codigo = CodigoErro.NAO_AUTORIZADO,
        mensagem = "Token válido, mas sem a permissão exigida por esta operação",
        requisicao = requisicao,
    )

    @ExceptionHandler(Exception::class)
    fun naoPrevisto(
        e: Exception,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Falha não prevista em {} (traceId={})", requisicao.requestURI, fabrica.traceIdAtual(), e)
        return fabrica.montar(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            codigo = CodigoErro.ERRO_INTERNO,
            mensagem = CodigoErro.ERRO_INTERNO.descricao,
            requisicao = requisicao,
        )
    }
}
