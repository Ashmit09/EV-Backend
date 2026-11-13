package com.voltx.exception;

public class VehicleNotFoundException extends ResourceNotFoundException {

    public VehicleNotFoundException(Object identifier) {
        super("Vehicle", identifier);
    }
}
