package com.voltx.dto.response;

import com.voltx.enums.Role;

import java.time.LocalDateTime;

/** Public view of a user. The password hash is intentionally absent. */
public record UserResponse(Long id, String fullName, String email, String phone, Role role,
                           boolean enabled, LocalDateTime createdAt) {
}
