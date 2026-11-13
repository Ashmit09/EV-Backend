package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** The booking request breaks a booking rule (duration, time window, opening hours...). */
public class InvalidBookingException extends VoltXException {

    public InvalidBookingException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String getErrorCode() {
        return "INVALID_BOOKING";
    }
}
