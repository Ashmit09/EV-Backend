package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** Login failed. The message never reveals whether the email or the password was wrong. */
public class InvalidCredentialsException extends VoltXException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String getErrorCode() {
        return "INVALID_CREDENTIALS";
    }
}
