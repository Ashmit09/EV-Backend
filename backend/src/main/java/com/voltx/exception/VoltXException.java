package com.voltx.exception;

import org.springframework.http.HttpStatus;

/**
 * Root of every business exception in VoltX.
 * <p>
 * Abstraction + inheritance: each subclass decides its own HTTP status and machine-readable
 * error code, so {@link GlobalExceptionHandler} can handle the whole family with one method.
 */
public abstract class VoltXException extends RuntimeException {

    protected VoltXException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();

    public abstract String getErrorCode();
}
