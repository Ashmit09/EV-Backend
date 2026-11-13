package com.voltx.mapper;

import com.voltx.dto.response.UserResponse;
import com.voltx.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
