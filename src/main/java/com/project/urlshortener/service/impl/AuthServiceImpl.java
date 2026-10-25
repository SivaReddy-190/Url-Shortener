package com.project.urlshortener.service.impl;

import com.project.urlshortener.dto.auth.AuthResponse;
import com.project.urlshortener.dto.auth.LoginRequest;
import com.project.urlshortener.dto.auth.RegisterRequest;
import com.project.urlshortener.model.User;
import com.project.urlshortener.repository.UserRepository;
import com.project.urlshortener.security.JwtService;
import com.project.urlshortener.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username()) || userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Registration failed. Username or email is already in use.");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername());
    }
}
