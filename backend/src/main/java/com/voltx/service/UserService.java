package com.voltx.service;

import com.voltx.dto.request.ChangePasswordRequest;
import com.voltx.dto.request.UpdateProfileRequest;
import com.voltx.dto.response.UserResponse;
import com.voltx.enums.Role;

import java.util.List;

public interface UserService {

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    List<UserResponse> listUsers(Role role);

    UserResponse setEnabled(String adminEmail, Long userId, boolean enabled);
}
