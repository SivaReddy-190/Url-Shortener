package com.project.urlshortener.service.impl;

import com.project.urlshortener.dto.auth.AuthResponse;
import com.project.urlshortener.dto.auth.LoginRequest;
import com.project.urlshortener.dto.auth.RegisterRequest;
import com.project.urlshortener.model.User;
import com.project.urlshortener.repository.UserRepository;
import com.project.urlshortener.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    // ─────────────────────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new RegisterRequest("newuser", "new@example.com", "securePass");
        }

        @Test
        @DisplayName("Should register a new user and return a JWT token")
        void shouldRegisterAndReturnToken() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("securePass")).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-string");

            AuthResponse response = authService.register(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-token-string");
            assertThat(response.username()).isEqualTo("newuser");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when username is already taken")
        void shouldThrowWhenUsernameAlreadyTaken() {
            when(userRepository.existsByUsername("newuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("newuser")
                    .hasMessageContaining("already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email is already registered")
        void shouldThrowWhenEmailAlreadyRegistered() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("new@example.com")
                    .hasMessageContaining("already registered");

            verify(userRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // login
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest validRequest;
        private User storedUser;

        @BeforeEach
        void setUp() {
            validRequest = new LoginRequest("existinguser", "correctPass");
            storedUser = User.builder()
                    .id(1L)
                    .username("existinguser")
                    .email("existing@example.com")
                    .password("hashed-password")
                    .build();
        }

        @Test
        @DisplayName("Should authenticate and return a JWT token on successful login")
        void shouldAuthenticateAndReturnToken() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(storedUser));
            when(jwtService.generateToken(storedUser)).thenReturn("login-jwt-token");

            AuthResponse response = authService.login(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("login-jwt-token");
            assertThat(response.username()).isEqualTo("existinguser");
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException on invalid credentials")
        void shouldPropagateExceptionOnBadCredentials() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("existinguser", "wrongPass")))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findByUsername(anyString());
        }
    }
}