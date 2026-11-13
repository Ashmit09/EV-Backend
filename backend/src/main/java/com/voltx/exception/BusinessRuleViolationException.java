package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** A domain rule was violated (invalid specification, deleting a vehicle with history...). */
public class BusinessRuleViolationException extends VoltXException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    @Override
    public String getErrorCode() {
        return "BUSINESS_RULE_VIOLATION";
    }
}
