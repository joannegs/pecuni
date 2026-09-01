package com.pecuni.common.exception;

import lombok.Getter;

/**
 * A business-rule violation that maps to {@code 409 Conflict} via
 * {@link ApiExceptionHandler} (contract 1.4). {@code errorType} becomes the
 * last path segment of the RFC 7807 {@code type} URI, e.g.
 * {@code "email-already-registered"} → {@code https://pecuni.app/erros/email-already-registered}.
 */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final String errorType;

    public BusinessRuleException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
}
