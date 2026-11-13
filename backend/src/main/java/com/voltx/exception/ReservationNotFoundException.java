package com.voltx.exception;

public class ReservationNotFoundException extends ResourceNotFoundException {

    public ReservationNotFoundException(Object identifier) {
        super("Reservation", identifier);
    }
}
