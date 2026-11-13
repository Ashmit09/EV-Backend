package com.voltx.exception;

public class StationNotFoundException extends ResourceNotFoundException {

    public StationNotFoundException(Object identifier) {
        super("Charging station", identifier);
    }
}
