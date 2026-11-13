package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** The requested slot overlaps an existing booking. */
public class BookingConflictException extends VoltXException {

    public BookingConflictException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getErrorCode() {
        return "BOOKING_CONFLICT";
    }
}
