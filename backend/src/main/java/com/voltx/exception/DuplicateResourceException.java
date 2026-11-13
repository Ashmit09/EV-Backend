package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** A unique value (email, registration number, charger code) is already taken. */
public class DuplicateResourceException extends VoltXException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getErrorCode() {
        return "DUPLICATE_RESOURCE";
    }
}
