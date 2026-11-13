package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** The authenticated user tried to act on a resource they do not own. */
public class UnauthorizedOperationException extends VoltXException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }

    @Override
    public String getErrorCode() {
        return "FORBIDDEN";
    }
}
