package com.voltx.exception;

public class InvoiceNotFoundException extends ResourceNotFoundException {

    public InvoiceNotFoundException(Object identifier) {
        super("Invoice", identifier);
    }
}
