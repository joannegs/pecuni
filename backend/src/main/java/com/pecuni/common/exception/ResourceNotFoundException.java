package com.pecuni.common.exception;

/** Maps to {@code 404} via {@link ApiExceptionHandler}. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
