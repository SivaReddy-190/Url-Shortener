package com.project.urlshortener.service;

import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.impl.ClickCountBufferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClickCountBufferService Unit Tests")
class ClickCountBufferServiceTest {

    @Mock
    private UrlRepository urlRepository;

    private ClickCountBufferServiceImpl bufferService;

    @BeforeEach
    void setUp() {
        bufferService = new ClickCountBufferServiceImpl(urlRepository);
    }

    // ─────────────────────────────────────────────────────────────
    // increment()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("increment()")
    class Increment {

        @Test
        @DisplayName("Should accumulate clicks for the same short link")
        void shouldAccumulateClicks() {
            bufferService.increment("abc123");
            bufferService.increment("abc123");
            bufferService.increment("abc123");

            assertThat(bufferService.getBufferedCount("abc123")).isEqualTo(3);
        }

        @Test
        @DisplayName("Should track clicks independently per short link")
        void shouldTrackClicksPerShortLink() {
            bufferService.increment("link-a");
            bufferService.increment("link-a");
            bufferService.increment("link-b");

            assertThat(bufferService.getBufferedCount("link-a")).isEqualTo(2);
            assertThat(bufferService.getBufferedCount("link-b")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return zero for non-existent short link")
        void shouldReturnZeroForNonExistent() {
            assertThat(bufferService.getBufferedCount("unknown")).isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // flushToDatabase()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("flushToDatabase()")
    class FlushToDatabase {

        @Test
        @DisplayName("Should call incrementClickCountByDelta with accumulated delta")
        void shouldFlushAccumulatedClicks() {
            bufferService.increment("abc123");
            bufferService.increment("abc123");
            bufferService.increment("abc123");

            bufferService.flushToDatabase();

            verify(urlRepository).incrementClickCountByDelta("abc123", 3);
        }

        @Test
        @DisplayName("Should flush multiple short links in a single flush cycle")
        void shouldFlushMultipleShortLinks() {
            bufferService.increment("link-a");
            bufferService.increment("link-a");
            bufferService.increment("link-b");
            bufferService.increment("link-c");
            bufferService.increment("link-c");
            bufferService.increment("link-c");

            bufferService.flushToDatabase();

            verify(urlRepository).incrementClickCountByDelta("link-a", 2);
            verify(urlRepository).incrementClickCountByDelta("link-b", 1);
            verify(urlRepository).incrementClickCountByDelta("link-c", 3);
        }

        @Test
        @DisplayName("Should clear buffer after successful flush")
        void shouldClearBufferAfterFlush() {
            bufferService.increment("abc123");
            bufferService.increment("abc123");

            bufferService.flushToDatabase();

            assertThat(bufferService.getBufferedCount("abc123")).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not call repository when buffer is empty")
        void shouldNotCallRepositoryWhenEmpty() {
            bufferService.flushToDatabase();

            verifyNoInteractions(urlRepository);
        }

        @Test
        @DisplayName("Should re-queue clicks on DB failure")
        void shouldReQueueOnFailure() {
            bufferService.increment("abc123");
            bufferService.increment("abc123");

            doThrow(new RuntimeException("DB down"))
                    .when(urlRepository).incrementClickCountByDelta("abc123", 2);

            bufferService.flushToDatabase();

            // Clicks should be re-queued for the next flush attempt
            assertThat(bufferService.getBufferedCount("abc123")).isEqualTo(2);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // shutdown()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("shutdown()")
    class Shutdown {

        @Test
        @DisplayName("Should flush remaining clicks on shutdown")
        void shouldFlushOnShutdown() {
            bufferService.increment("abc123");
            bufferService.increment("abc123");

            bufferService.shutdown();

            verify(urlRepository).incrementClickCountByDelta("abc123", 2);
            assertThat(bufferService.getBufferedCount("abc123")).isEqualTo(0);
        }
    }
}
