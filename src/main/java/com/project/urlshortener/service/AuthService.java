package com.project.urlshortener.service;

import com.project.urlshortener.dto.auth.AuthResponse;
import com.project.urlshortener.dto.auth.LoginRequest;
import com.project.urlshortener.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
