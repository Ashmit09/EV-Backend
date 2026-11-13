package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** The vehicle cannot charge on the selected charging unit. */
public class IncompatibleConnectorException extends VoltXException {

    public IncompatibleConnectorException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    @Override
    public String getErrorCode() {
        return "INCOMPATIBLE_CONNECTOR";
    }
}
