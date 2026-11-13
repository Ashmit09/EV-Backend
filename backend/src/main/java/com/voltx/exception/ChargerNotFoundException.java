package com.voltx.exception;

public class ChargerNotFoundException extends ResourceNotFoundException {

    public ChargerNotFoundException(Object identifier) {
        super("Charger", identifier);
    }
}
