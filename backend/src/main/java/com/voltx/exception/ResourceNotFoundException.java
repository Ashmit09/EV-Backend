package com.voltx.exception;

import org.springframework.http.HttpStatus;

/** Parent of all "entity not found" errors; subclasses only supply the resource name. */
public abstract class ResourceNotFoundException extends VoltXException {

    private final String resource;

    protected ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " " + identifier + " was not found");
        this.resource = resource;
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorCode() {
        return resource.toUpperCase().replace(' ', '_') + "_NOT_FOUND";
    }
}
