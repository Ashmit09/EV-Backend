package com.voltx.controller;

import com.voltx.dto.request.ChangePasswordRequest;
import com.voltx.dto.request.UpdateProfileRequest;
import com.voltx.dto.response.NotificationResponse;
import com.voltx.dto.response.UserResponse;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.NotificationService;
import com.voltx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Profile & notifications")
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService;

    public UserController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping("/api/users/me")
    @Operation(summary = "Current user's profile")
    public UserResponse me(@AuthenticationPrincipal VoltXUserDetails me) {
        return userService.getProfile(me.getId());
    }

    @PutMapping("/api/users/me")
    @Operation(summary = "Update name and phone")
    public UserResponse update(@AuthenticationPrincipal VoltXUserDetails me,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(me.getId(), request);
    }

    @PutMapping("/api/users/me/password")
    @Operation(summary = "Change password (requires the current password)")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal VoltXUserDetails me,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(me.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/notifications")
    @Operation(summary = "Latest in-app notifications")
    public List<NotificationResponse> notifications(@AuthenticationPrincipal VoltXUserDetails me) {
        return notificationService.latestFor(me.getUsername());
    }
}
