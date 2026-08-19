package com.project.urlshortener.service.impl;

import com.project.urlshortener.dto.url.UrlDto;
import com.project.urlshortener.exception.AccessDeniedException;
import com.project.urlshortener.exception.ShortLinkCollisionException;
import com.project.urlshortener.exception.UrlExpiredException;
import com.project.urlshortener.exception.UrlNotFoundException;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.User;
import com.project.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlServiceImpl Unit Tests")
class UrlServiceImplTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ClickCountBufferServiceImpl clickCountBufferServiceImpl;

    @InjectMocks
    private UrlServiceImpl urlService;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .password("encoded-password")
                .build();

        anotherUser = User.builder()
                .id(2L)
                .username("anotheruser")
                .email("another@example.com")
                .password("encoded-password")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // generateShortLink
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("generateShortLink()")
    class GenerateShortLink {

        @Test
        @DisplayName("Should auto-generate a short link when none is provided")
        void shouldAutoGenerateShortLink() {
            UrlDto urlDto = new UrlDto("https://example.com", 10, null);

            Url savedUrl = Url.builder()
                    .shortLink("abc12345")
                    .originalUrl("https://example.com")
                    .createdTime(LocalDateTime.now())
                    .expirationTime(LocalDateTime.now().plusDays(10))
                    .clickCount(0L)
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink(any())).thenReturn(Optional.empty());
            when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

            Url result = urlService.generateShortLink(urlDto, testUser);

            assertThat(result).isNotNull();
            assertThat(result.getShortLink()).isEqualTo("abc12345");
            assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
            verify(urlRepository).save(any(Url.class));
        }

        @Test
        @DisplayName("Should use provided custom short link when available")
        void shouldUseCustomShortLink() {
            UrlDto urlDto = new UrlDto("https://example.com", 30, "my-link");

            Url savedUrl = Url.builder()
                    .shortLink("my-link")
                    .originalUrl("https://example.com")
                    .createdTime(LocalDateTime.now())
                    .expirationTime(LocalDateTime.now().plusDays(30))
                    .clickCount(0L)
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink("my-link")).thenReturn(Optional.empty());
            when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

            Url result = urlService.generateShortLink(urlDto, testUser);

            assertThat(result.getShortLink()).isEqualTo("my-link");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when custom short link is already taken")
        void shouldThrowWhenCustomShortLinkTaken() {
            UrlDto urlDto = new UrlDto("https://example.com", null, "taken");

            Url existingUrl = Url.builder().shortLink("taken").build();
            when(urlRepository.findByShortLink("taken")).thenReturn(Optional.of(existingUrl));

            assertThatThrownBy(() -> urlService.generateShortLink(urlDto, testUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taken");

            verify(urlRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ShortLinkCollisionException after max collision retries")
        void shouldThrowAfterMaxCollisionRetries() {
            UrlDto urlDto = new UrlDto("https://example.com", null, null);

            // Every generated short link collides
            when(urlRepository.findByShortLink(any())).thenReturn(Optional.of(Url.builder().build()));

            assertThatThrownBy(() -> urlService.generateShortLink(urlDto, testUser))
                    .isInstanceOf(ShortLinkCollisionException.class)
                    .hasMessageContaining("Failed to generate a unique short link");
        }

        @Test
        @DisplayName("Should use provided expirationDays for expiration time")
        void shouldRespectCustomExpirationDays() {
            UrlDto urlDto = new UrlDto("https://example.com", 7, null);

            when(urlRepository.findByShortLink(any())).thenReturn(Optional.empty());
            when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

            Url result = urlService.generateShortLink(urlDto, testUser);

            // Expiration should be ~7 days from now
            assertThat(result.getExpirationTime())
                    .isAfter(LocalDateTime.now().plusDays(6))
                    .isBefore(LocalDateTime.now().plusDays(8));
        }

        @Test
        @DisplayName("Should default to 60 days expiration when expirationDays is null")
        void shouldDefaultTo60DaysWhenExpirationDaysNull() {
            UrlDto urlDto = new UrlDto("https://example.com", null, null);

            when(urlRepository.findByShortLink(any())).thenReturn(Optional.empty());
            when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

            Url result = urlService.generateShortLink(urlDto, testUser);

            assertThat(result.getExpirationTime())
                    .isAfter(LocalDateTime.now().plusDays(59))
                    .isBefore(LocalDateTime.now().plusDays(61));
        }

        @Test
        @DisplayName("Should default to 60 days expiration when expirationDays is zero or negative")
        void shouldDefaultTo60DaysWhenExpirationDaysZeroOrNegative() {
            UrlDto urlDto = new UrlDto("https://example.com", 0, null);

            when(urlRepository.findByShortLink(any())).thenReturn(Optional.empty());
            when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

            Url result = urlService.generateShortLink(urlDto, testUser);

            assertThat(result.getExpirationTime())
                    .isAfter(LocalDateTime.now().plusDays(59))
                    .isBefore(LocalDateTime.now().plusDays(61));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getEncodedUrl
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getEncodedUrl()")
    class GetEncodedUrl {

        @Test
        @DisplayName("Should return URL and increment click count for a valid, non-expired link")
        void shouldReturnUrlAndIncrementClickCount() {
            Url url = Url.builder()
                    .shortLink("abc12345")
                    .originalUrl("https://example.com")
                    .expirationTime(LocalDateTime.now().plusDays(10))
                    .clickCount(5L)
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink("abc12345")).thenReturn(Optional.of(url));

            Url result = urlService.getEncodedUrl("abc12345");

            assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
            verify(clickCountBufferServiceImpl).increment("abc12345");
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException when short link does not exist")
        void shouldThrowWhenShortLinkNotFound() {
            when(urlRepository.findByShortLink("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> urlService.getEncodedUrl("unknown"))
                    .isInstanceOf(UrlNotFoundException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("Should delete expired URL and throw UrlExpiredException")
        void shouldDeleteAndThrowWhenUrlExpired() {
            Url expiredUrl = Url.builder()
                    .shortLink("expired")
                    .originalUrl("https://old.com")
                    .expirationTime(LocalDateTime.now().minusDays(1))
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink("expired")).thenReturn(Optional.of(expiredUrl));

            assertThatThrownBy(() -> urlService.getEncodedUrl("expired"))
                    .isInstanceOf(UrlExpiredException.class)
                    .hasMessageContaining("expired");

            verify(urlRepository).delete(expiredUrl);
            verify(clickCountBufferServiceImpl, never()).increment(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getOriginalUrl
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getOriginalUrl()")
    class GetOriginalUrl {

        @Test
        @DisplayName("Should return URL when called by the owner")
        void shouldReturnUrlForOwner() {
            Url url = Url.builder()
                    .shortLink("mylink")
                    .originalUrl("https://example.com")
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink("mylink")).thenReturn(Optional.of(url));

            Url result = urlService.getOriginalUrl("mylink", testUser);

            assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when called by a non-owner")
        void shouldThrowForNonOwner() {
            Url url = Url.builder()
                    .shortLink("mylink")
                    .originalUrl("https://example.com")
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink("mylink")).thenReturn(Optional.of(url));

            assertThatThrownBy(() -> urlService.getOriginalUrl("mylink", anotherUser))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("do not own");
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException when link does not exist")
        void shouldThrowWhenLinkNotFound() {
            when(urlRepository.findByShortLink("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> urlService.getOriginalUrl("ghost", testUser))
                    .isInstanceOf(UrlNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // deleteShortLink
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteShortLink()")
    class DeleteShortLink {

        @Test
        @DisplayName("Should delete the URL when called by the owner")
        void shouldDeleteWhenCalledByOwner() {
            Url url = Url.builder()
                    .shortLink("to-delete")
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink("to-delete")).thenReturn(Optional.of(url));

            urlService.deleteShortLink("to-delete", testUser);

            verify(urlRepository).delete(url);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when non-owner tries to delete")
        void shouldThrowForNonOwner() {
            Url url = Url.builder()
                    .shortLink("to-delete")
                    .user(testUser)
                    .build();

            when(urlRepository.findByShortLink("to-delete")).thenReturn(Optional.of(url));

            assertThatThrownBy(() -> urlService.deleteShortLink("to-delete", anotherUser))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("do not own");

            verify(urlRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException when link does not exist")
        void shouldThrowWhenLinkNotFound() {
            when(urlRepository.findByShortLink("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> urlService.deleteShortLink("missing", testUser))
                    .isInstanceOf(UrlNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getUserLinks
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getUserLinks()")
    class GetUserLinks {

        @Test
        @DisplayName("Should return all links belonging to the user")
        void shouldReturnUserLinks() {
            List<Url> links = List.of(
                    Url.builder().shortLink("link1").user(testUser).build(),
                    Url.builder().shortLink("link2").user(testUser).build()
            );

            when(urlRepository.findByUser(testUser)).thenReturn(links);

            List<Url> result = urlService.getUserLinks(testUser);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Url::getShortLink).containsExactly("link1", "link2");
        }

        @Test
        @DisplayName("Should return empty list when user has no links")
        void shouldReturnEmptyListWhenNoLinks() {
            when(urlRepository.findByUser(testUser)).thenReturn(Collections.emptyList());

            List<Url> result = urlService.getUserLinks(testUser);

            assertThat(result).isEmpty();
        }
    }
}