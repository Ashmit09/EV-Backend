package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** The charger (or its station) cannot be used right now. */
public class ChargerUnavailableException extends VoltXException {

    public ChargerUnavailableException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getErrorCode() {
        return "CHARGER_UNAVAILABLE";
    }
}
