package com.pecuni.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Single, global RFC 7807 (contract 1.4) exception handler. Every new
 * business error should be expressed as a {@link BusinessRuleException} with
 * a new {@code errorType} rather than a new handler method here.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "validacao", "Erro de validação",
                "Um ou mais campos são inválidos.", request.getRequestURI());
        List<CampoErro> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toCampoErro)
                .toList();
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedBody(HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "corpo-invalido", "Corpo da requisição inválido",
                "O corpo da requisição não pôde ser interpretado.", request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "recurso-nao-encontrado", "Recurso não encontrado",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getErrorType(), "Violação de regra de negócio",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno do servidor",
                "Ocorreu um erro inesperado.", request.getRequestURI());
    }

    private static CampoErro toCampoErro(FieldError fieldError) {
        return new CampoErro(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private record CampoErro(String campo, String mensagem) {
    }
}
