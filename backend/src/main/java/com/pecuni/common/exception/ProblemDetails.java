package com.pecuni.common.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Shared RFC 7807 (contract 1.4) problem builder. Used both by
 * {@link ApiExceptionHandler} (exceptions raised inside the DispatcherServlet)
 * and by the Spring Security entry point / access-denied handler in
 * {@code config.SecurityConfig} (failures raised in the filter chain, before
 * the DispatcherServlet, which never reach an {@code @ExceptionHandler}).
 */
public final class ProblemDetails {

    private static final String ERROR_BASE_URI = "https://pecuni.app/erros/";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String errorType, String title, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(ERROR_BASE_URI + errorType));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(instance));
        return problem;
    }
}
