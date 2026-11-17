package com.project.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.urlshortener.dto.url.UrlDto;
import com.project.urlshortener.exception.AccessDeniedException;
import com.project.urlshortener.exception.UrlNotFoundException;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.repository.UserRepository;
import com.project.urlshortener.security.JwtService;
import com.project.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for UrlController.
 *
 * addFilters=false disables servlet filters so JWT authentication is bypassed.
 * @WithMockUser sets the SecurityContext via TestExecutionListener (before the
 * test runs), which works even with addFilters=false.
 *
 * @AuthenticationPrincipal resolves to null because @WithMockUser injects
 * Spring's built-in User type which is not assignable to our custom User entity
 * (errorOnInvalidType defaults to false → graceful null).
 * Stubs therefore use any() for the user argument to match null.
 *
 * Minimum @MockitoBeans to boot the @WebMvcTest context:
 *  - JwtService     → satisfies JwtAuthenticationFilter constructor (@Component)
 *  - UserRepository → satisfies ApplicationConfig constructor (@Configuration)
 *  - UrlService     → the controller dependency under test
 *
 * NOTE: @MockitoBean (org.springframework.test.context.bean.override.mockito)
 * is the Spring Boot 3.4+ replacement for the deprecated @MockBean.
 */
@WebMvcTest(UrlController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UrlController Web Layer Tests")
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @MockitoBean
    private UrlService urlService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private Url sampleUrl;

    @BeforeEach
    void setUp() {
        sampleUrl = Url.builder()
                .shortLink("abc12345")
                .originalUrl("https://example.com")
                .createdTime(LocalDateTime.now())
                .expirationTime(LocalDateTime.now().plusDays(30))
                .clickCount(42L)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/v1/urls/generate
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/urls/generate")
    class GenerateShortLink {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with UrlResponseDto on valid request")
        void shouldReturn200WithResponseDto() throws Exception {
            UrlDto urlDto = new UrlDto("https://example.com", 30, null);
            when(urlService.generateShortLink(any(UrlDto.class), any())).thenReturn(sampleUrl);

            mockMvc.perform(post("/api/v1/urls/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(urlDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortLink").value("abc12345"))
                    .andExpect(jsonPath("$.originalUrl").value("https://example.com"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when URL body is blank")
        void shouldReturn400WhenUrlIsBlank() throws Exception {
            UrlDto invalidDto = new UrlDto("", 30, null);

            mockMvc.perform(post("/api/v1/urls/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/urls/stats/{shortLink}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/urls/stats/{shortLink}")
    class GetStats {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with click count map")
        void shouldReturnClickCount() throws Exception {
            when(urlService.getOriginalUrl(eq("abc12345"), any())).thenReturn(sampleUrl);

            mockMvc.perform(get("/api/v1/urls/stats/abc12345"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clickCount").value(42));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 403 when user does not own the link")
        void shouldReturn403WhenNotOwner() throws Exception {
            when(urlService.getOriginalUrl(eq("abc12345"), any()))
                    .thenThrow(new AccessDeniedException("You do not own this link"));

            mockMvc.perform(get("/api/v1/urls/stats/abc12345"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /api/v1/urls/{shortLink}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/v1/urls/{shortLink}")
    class DeleteShortLink {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with success message")
        void shouldReturn200OnSuccessfulDelete() throws Exception {
            // void mock: doNothing() is the default, no explicit stub needed
            mockMvc.perform(delete("/api/v1/urls/abc12345"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("URL deleted successfully"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when link does not exist")
        void shouldReturn404WhenLinkNotFound() throws Exception {
            doThrow(new UrlNotFoundException("URL not found for short link: missing"))
                    .when(urlService).deleteShortLink(eq("missing"), any());

            mockMvc.perform(delete("/api/v1/urls/missing"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/urls/my-links
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/urls/my-links")
    class GetMyLinks {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with list of user links")
        void shouldReturnListOfLinks() throws Exception {
            Url link2 = Url.builder()
                    .shortLink("xyz98765")
                    .originalUrl("https://other.com")
                    .expirationTime(LocalDateTime.now().plusDays(10))
                    .build();

            when(urlService.getUserLinks(any())).thenReturn(List.of(sampleUrl, link2));

            mockMvc.perform(get("/api/v1/urls/my-links"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].shortLink").value("abc12345"))
                    .andExpect(jsonPath("$[1].shortLink").value("xyz98765"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/v1/urls/{shortLink}/extend-expiry
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/v1/urls/{shortLink}/extend-expiry")
    class ExtendExpiry {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with updated expiration time via query param")
        void shouldExtendExpirySuccessfully() throws Exception {
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(40);
            Url updated = Url.builder()
                    .shortLink("abc12345")
                    .originalUrl("https://example.com/target")
                    .expirationTime(newExpiry)
                    .build();

            when(urlService.extendExpiration(eq("abc12345"), eq(30), any())).thenReturn(updated);

            mockMvc.perform(patch("/api/v1/urls/abc12345/extend-expiry?days=30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortLink").value("abc12345"))
                    .andExpect(jsonPath("$.expirationTime").isNotEmpty());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with updated expiration time via JSON body")
        void shouldExtendExpiryViaJsonBody() throws Exception {
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(60);
            Url updated = Url.builder()
                    .shortLink("abc12345")
                    .originalUrl("https://example.com/target")
                    .expirationTime(newExpiry)
                    .build();

            when(urlService.extendExpiration(eq("abc12345"), eq(60), any())).thenReturn(updated);

            mockMvc.perform(patch("/api/v1/urls/abc12345/extend-expiry")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"days\": 60}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortLink").value("abc12345"))
                    .andExpect(jsonPath("$.expirationTime").isNotEmpty());
        }
    }
}
