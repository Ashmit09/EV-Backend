package com.voltx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @Pattern(regexp = "^$|^[+]?[0-9]{10,13}$", message = "must be a valid phone number") String phone) {
}
