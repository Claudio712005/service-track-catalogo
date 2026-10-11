package com.clau.service_track.catalogo.infrastructure.adapter.web.error

import com.clau.service_track.catalogo.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.catalogo.domain.exception.ConflitoDeEstadoException
import com.clau.service_track.catalogo.domain.exception.DomainException
import jakarta.validation.ConstraintViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler(FabricaDeErro())

    private val requisicao = MockHttpServletRequest("GET", "/insumos/abc").apply {
        requestURI = "/insumos/abc"
    }

    private class Corpo(val sku: String = "")

    private fun parametro(): MethodParameter =
        MethodParameter(Corpo::class.java.getDeclaredConstructor(String::class.java), 0)

    @Test
    fun `corpo invalido vira 400 com uma violacao por campo`() {
        val resultado = BeanPropertyBindingResult(Corpo(), "corpo")
        resultado.addError(FieldError("corpo", "sku", "", false, null, null, "nao pode ser vazio"))
        resultado.addError(FieldError("corpo", "custo", "-1", false, null, null, "deve ser positivo"))

        val resposta = handler.corpoInvalido(
            MethodArgumentNotValidException(parametro(), resultado),
            requisicao,
        )

        assertEquals(HttpStatus.BAD_REQUEST, resposta.statusCode)
        val corpo = resposta.body!!
        assertEquals(CodigoErro.REQUISICAO_INVALIDA, corpo.code)
        assertEquals(2, corpo.violacoes?.size)
        assertTrue(corpo.message.contains("2 campo(s)"))
        assertEquals("/insumos/abc", corpo.path)

        val custo = corpo.violacoes!!.single { it.campo == "custo" }
        assertEquals("deve ser positivo", custo.mensagem)
        assertEquals("-1", custo.valorRejeitado)
    }

    @Test
    fun `violacao sem mensagem recebe texto padrao, e nao nulo`() {
        val resultado = BeanPropertyBindingResult(Corpo(), "corpo")
        resultado.addError(FieldError("corpo", "sku", null, false, null, null, null))

        val corpo = handler.corpoInvalido(
            MethodArgumentNotValidException(parametro(), resultado),
            requisicao,
        ).body!!

        assertEquals("Valor inválido", corpo.violacoes!!.single().mensagem)
        assertNull(corpo.violacoes!!.single().valorRejeitado)
    }

    @Test
    fun `parametro invalido vira 400 com o nome curto do campo`() {
        val violacoes = emptySet<jakarta.validation.ConstraintViolation<*>>()

        val resposta = handler.parametroInvalido(ConstraintViolationException(violacoes), requisicao)

        assertEquals(HttpStatus.BAD_REQUEST, resposta.statusCode)
        assertEquals(CodigoErro.REQUISICAO_INVALIDA, resposta.body!!.code)
    }

    @Test
    fun `corpo ilegivel vira 400 com codigo proprio, e nao expoe a causa`() {
        val resposta = handler.corpoIlegivel(
            HttpMessageNotReadableException("falha", MockHttpInputMessage(ByteArray(0))),
            requisicao,
        )

        assertEquals(HttpStatus.BAD_REQUEST, resposta.statusCode)
        assertEquals(CodigoErro.CORPO_ILEGIVEL, resposta.body!!.code)
        assertTrue(
            !resposta.body!!.message.contains("falha"),
            "a mensagem interna do parser nao vai para o cliente",
        )
    }

    @Test
    fun `cabecalho obrigatorio ausente vira 400 nomeando o cabecalho`() {
        val resposta = handler.cabecalhoAusente(
            MissingRequestHeaderException("X-API-Version", parametro()),
            requisicao,
        )

        assertEquals(HttpStatus.BAD_REQUEST, resposta.statusCode)
        assertTrue(resposta.body!!.message.contains("X-API-Version"))
    }

    @Test
    fun `tipo incompativel no parametro vira 400`() {
        val resposta = handler.tipoIncompativel(
            MethodArgumentTypeMismatchException("abc", Int::class.java, "pagina", parametro(), null),
            requisicao,
        )

        assertEquals(HttpStatus.BAD_REQUEST, resposta.statusCode)
        assertEquals(CodigoErro.PARAMETRO_INVALIDO, resposta.body!!.code)
    }

    @Test
    fun `conflito de estado vira 409 e preserva a mensagem do dominio`() {
        val resposta = handler.conflitoDeEstado(
            ConflitoDeEstadoException("Reserva ja esta consumida"),
            requisicao,
        )

        assertEquals(HttpStatus.CONFLICT, resposta.statusCode)
        assertEquals(CodigoErro.CONFLITO_DE_ESTADO, resposta.body!!.code)
        assertEquals("Reserva ja esta consumida", resposta.body!!.message)
    }

    @Test
    fun `regra de negocio vira 400 e preserva a mensagem`() {
        val resposta = handler.regraDeNegocio(DomainException("Saldo insuficiente"), requisicao)

        assertEquals(HttpStatus.BAD_REQUEST, resposta.statusCode)
        assertEquals(CodigoErro.REGRA_DE_NEGOCIO, resposta.body!!.code)
        assertEquals("Saldo insuficiente", resposta.body!!.message)
    }

    @Test
    fun `recurso inexistente vira 404`() {
        val resposta = handler.naoEncontrado(
            RecursoNaoEncontradoException("Insumo", "abc"),
            requisicao,
        )

        assertEquals(HttpStatus.NOT_FOUND, resposta.statusCode)
        assertEquals(CodigoErro.RECURSO_NAO_ENCONTRADO, resposta.body!!.code)
    }

    @Test
    fun `rota inexistente vira 404, e nao 500`() {
        val resposta = handler.rotaInexistente(
            NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/nao-existe", "/nao-existe"),
            requisicao,
        )

        assertEquals(HttpStatus.NOT_FOUND, resposta.statusCode)
        assertEquals(CodigoErro.RECURSO_NAO_ENCONTRADO, resposta.body!!.code)
    }

    @Test
    fun `metodo nao suportado vira 405`() {
        val resposta = handler.metodoNaoSuportado(
            HttpRequestMethodNotSupportedException("DELETE"),
            requisicao,
        )

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resposta.statusCode)
        assertEquals(CodigoErro.METODO_NAO_SUPORTADO, resposta.body!!.code)
    }

    @Test
    fun `falha de autenticacao vira 401 e nao conta o motivo`() {
        val resposta = handler.naoAutenticado(
            BadCredentialsException("assinatura nao confere com a chave kid=abc123"),
            requisicao,
        )

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.statusCode)
        assertEquals(CodigoErro.NAO_AUTENTICADO, resposta.body!!.code)
        assertTrue(
            !resposta.body!!.message.contains("kid=abc123"),
            "o detalhe da falha de credencial nao pode sair na resposta: ${resposta.body!!.message}",
        )
    }

    @Test
    fun `acesso negado vira 403`() {
        val resposta = handler.naoAutorizado(AccessDeniedException("sem papel"), requisicao)

        assertEquals(HttpStatus.FORBIDDEN, resposta.statusCode)
        assertEquals(CodigoErro.NAO_AUTORIZADO, resposta.body!!.code)
    }

    @Test
    fun `falha nao prevista vira 500 sem vazar a excecao`() {
        val resposta = handler.naoPrevisto(IllegalStateException("conexao recusada em 10.0.0.5"), requisicao)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.statusCode)
        assertEquals(CodigoErro.ERRO_INTERNO, resposta.body!!.code)
        assertEquals(CodigoErro.ERRO_INTERNO.descricao, resposta.body!!.message)
        assertTrue(
            !resposta.body!!.message.contains("10.0.0.5"),
            "endereco interno nao pode sair na resposta",
        )
    }

    @Test
    fun `sem span ativo o traceId sai nulo, em vez de um identificador invalido`() {
        assertNull(FabricaDeErro().traceIdAtual())
    }

    @Test
    fun `toda resposta de erro carrega status, error e caminho`() {
        val corpo = handler.naoEncontrado(RecursoNaoEncontradoException("Insumo", "abc"), requisicao).body!!

        assertEquals(404, corpo.status)
        assertEquals(HttpStatus.NOT_FOUND.reasonPhrase, corpo.error)
        assertEquals("/insumos/abc", corpo.path)
        assertNull(corpo.violacoes, "erro sem violacao de campo nao inventa lista vazia")
    }
}
