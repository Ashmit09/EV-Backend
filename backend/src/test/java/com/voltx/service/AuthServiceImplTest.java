package com.voltx.service;

import com.voltx.dto.request.LoginRequest;
import com.voltx.dto.request.RegisterRequest;
import com.voltx.exception.DuplicateResourceException;
import com.voltx.exception.InvalidCredentialsException;
import com.voltx.repository.UserRepository;
import com.voltx.security.JwtService;
import com.voltx.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void duplicateEmailIsRejectedAndNothingIsSaved() {
        when(userRepository.existsByEmailIgnoreCase("taken@voltx.in")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Someone", "Taken@VoltX.in", "Password1", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void wrongPasswordGivesAGenericMessage() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThatThrownBy(() -> authService.login(new LoginRequest("a@voltx.in", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
