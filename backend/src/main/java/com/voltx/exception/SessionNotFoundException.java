package com.voltx.exception;

public class SessionNotFoundException extends ResourceNotFoundException {

    public SessionNotFoundException(Object identifier) {
        super("Charging session", identifier);
    }
}
