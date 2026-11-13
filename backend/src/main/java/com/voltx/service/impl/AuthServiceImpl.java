package com.voltx.service.impl;

import com.voltx.dto.request.LoginRequest;
import com.voltx.dto.request.RegisterRequest;
import com.voltx.dto.response.AuthResponse;
import com.voltx.entity.User;
import com.voltx.enums.AuditAction;
import com.voltx.enums.Role;
import com.voltx.exception.DuplicateResourceException;
import com.voltx.exception.InvalidCredentialsException;
import com.voltx.mapper.UserMapper;
import com.voltx.repository.UserRepository;
import com.voltx.security.JwtService;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.AuditService;
import com.voltx.service.AuthService;
import com.voltx.service.NotificationService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    /** Constructor injection: dependencies are explicit, final and easy to mock in tests. */
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService,
                           AuditService auditService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        User user = userRepository.save(new User(request.fullName(), email,
                passwordEncoder.encode(request.password()), phone, Role.CUSTOMER));

        auditService.record(user.getEmail(), AuditAction.USER_REGISTERED, "User", user.getId(), "Customer account created");
        notificationService.notify(user, "Welcome to VoltX",
                "Hi " + user.getFullName() + ", your VoltX account is ready. Add your EV to start booking chargers.");
        return tokenFor(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        } catch (DisabledException ex) {
            throw new InvalidCredentialsException("This account has been disabled. Please contact support.");
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        VoltXUserDetails principal = (VoltXUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        auditService.record(user.getEmail(), AuditAction.USER_LOGIN, "User", user.getId(), "Signed in");
        return tokenFor(user);
    }

    private AuthResponse tokenFor(User user) {
        JwtService.IssuedToken token = jwtService.issue(new VoltXUserDetails(user));
        return new AuthResponse(token.token(), "Bearer", token.expiresAt(), UserMapper.toResponse(user));
    }
}
