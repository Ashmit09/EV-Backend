package com.voltx.service.impl;

import com.voltx.dto.request.ChangePasswordRequest;
import com.voltx.dto.request.UpdateProfileRequest;
import com.voltx.dto.response.UserResponse;
import com.voltx.entity.User;
import com.voltx.enums.AuditAction;
import com.voltx.enums.Role;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.exception.UserNotFoundException;
import com.voltx.mapper.UserMapper;
import com.voltx.repository.UserRepository;
import com.voltx.service.AuditService;
import com.voltx.service.UserService;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserMapper.toResponse(findUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        user.updateProfile(request.fullName(), phone);
        auditService.record(user.getEmail(), AuditAction.PROFILE_UPDATED, "User", userId, "Profile details updated");
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException("New password must be different from the current password");
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.record(user.getEmail(), AuditAction.PASSWORD_CHANGED, "User", userId, "Password changed");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Role role) {
        List<User> users = role == null
                ? userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : userRepository.findByRoleOrderByCreatedAtDesc(role);
        return users.stream().map(UserMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse setEnabled(String adminEmail, Long userId, boolean enabled) {
        User user = findUser(userId);
        if (user.isAdmin()) {
            throw new BusinessRuleViolationException("Administrator accounts cannot be disabled from the console");
        }
        if (enabled) {
            user.enable();
        } else {
            user.disable();
        }
        auditService.record(adminEmail, enabled ? AuditAction.USER_ENABLED : AuditAction.USER_DISABLED,
                "User", userId, user.getEmail());
        return UserMapper.toResponse(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
