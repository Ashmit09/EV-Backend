package com.voltx.service;

import com.voltx.dto.request.LoginRequest;
import com.voltx.dto.request.RegisterRequest;
import com.voltx.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
