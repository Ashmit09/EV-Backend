package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** A charging session operation that is not valid in the session's current state. */
public class InvalidSessionException extends VoltXException {

    public InvalidSessionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getErrorCode() {
        return "INVALID_SESSION";
    }
}
