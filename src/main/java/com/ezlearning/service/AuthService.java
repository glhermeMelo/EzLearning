package com.ezlearning.service;

import com.ezlearning.model.dto.LoginRequest;
import com.ezlearning.model.dto.LoginResponse;
import com.ezlearning.model.dto.RegisterRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    LoginResponse refreshToken(String refreshToken);
}
