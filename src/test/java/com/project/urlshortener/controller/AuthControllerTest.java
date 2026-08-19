package com.project.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.urlshortener.dto.auth.AuthResponse;
import com.project.urlshortener.dto.auth.LoginRequest;
import com.project.urlshortener.dto.auth.RegisterRequest;
import com.project.urlshortener.repository.UserRepository;
import com.project.urlshortener.security.JwtService;
import com.project.urlshortener.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for AuthController.
 *
 * addFilters=false disables all servlet filters so auth endpoints (which are
 * permitAll) receive requests directly without needing any security context.
 *
 * Minimum @MockitoBeans needed to boot the @WebMvcTest application context:
 *  - JwtService     → satisfies JwtAuthenticationFilter constructor (@Component)
 *  - UserRepository → satisfies ApplicationConfig constructor (@Configuration)
 *  - AuthService    → the controller dependency under test
 *
 * NOTE: @MockitoBean (org.springframework.test.context.bean.override.mockito)
 * is the Spring Boot 3.4+ replacement for the deprecated @MockBean.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** The controller dependency under test — registered as a Spring bean by @MockitoBean. */
    @MockitoBean
    private AuthService authService;

    /** Satisfies JwtAuthenticationFilter(@Component) constructor dependency. */
    @MockitoBean
    private JwtService jwtService;

    /** Satisfies ApplicationConfig(@Configuration) constructor dependency. */
    @MockitoBean
    private UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────
    // POST /api/v1/auth/register  (permitAll)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("Should return 200 with token on successful registration")
        void shouldReturn200WithToken() throws Exception {
            RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "securePass");
            AuthResponse response = new AuthResponse("jwt-token", "newuser");

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.username").value("newuser"));
        }

        @Test
        @DisplayName("Should return 400 when username is already taken")
        void shouldReturn400WhenUsernameTaken() throws Exception {
            RegisterRequest request = new RegisterRequest("takenuser", "new@example.com", "securePass");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("Username 'takenuser' is already taken"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Username 'takenuser' is already taken"));
        }

        @Test
        @DisplayName("Should return 400 when request body fails bean validation")
        void shouldReturn400WhenBodyInvalid() throws Exception {
            // blank username violates @NotBlank; password too short violates @Size(min=6)
            RegisterRequest invalidRequest = new RegisterRequest("", "invalid-email", "123");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/v1/auth/login  (permitAll)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Should return 200 with token on successful login")
        void shouldReturn200WithToken() throws Exception {
            LoginRequest request = new LoginRequest("existinguser", "correctPass");
            AuthResponse response = new AuthResponse("login-jwt-token", "existinguser");

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("login-jwt-token"))
                    .andExpect(jsonPath("$.username").value("existinguser"));
        }

        @Test
        @DisplayName("Should return 401 on bad credentials")
        void shouldReturn401OnBadCredentials() throws Exception {
            LoginRequest request = new LoginRequest("existinguser", "wrongPass");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
