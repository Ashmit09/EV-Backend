package com.voltx.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(Object identifier) {
        super("User", identifier);
    }
}
