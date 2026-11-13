package com.voltx.enums;

/** Application roles. Spring Security sees them as ROLE_CUSTOMER / ROLE_ADMIN. */
public enum Role {
    CUSTOMER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
