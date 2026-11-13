package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** A charger state transition that the state machine does not allow. */
public class InvalidChargingStateException extends VoltXException {

    public InvalidChargingStateException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getErrorCode() {
        return "INVALID_CHARGER_STATE";
    }
}
