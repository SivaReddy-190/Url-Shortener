package com.project.urlshortener.service;

import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.impl.ClickCountBufferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClickCountBufferService Unit Tests")
class ClickCountBufferServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Cursor<String> cursor;

    private ClickCountBufferService bufferService;

    @Mock
    private ClickCountFlushService flushService;

    @BeforeEach
    void setUp() {
        bufferService = spy(new ClickCountBufferServiceImpl(flushService, redisTemplate));
    }

    /** Helper: configure the mock cursor to return the given keys */
    private void mockScanKeys(String... keys) {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> action = inv.getArgument(0);
            for (String key : keys) {
                action.accept(key);
            }
            return null;
        }).when(cursor).forEachRemaining(any());
    }

    /** Helper: configure an empty scan result */
    private void mockScanEmpty() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    }

    // ─────────────────────────────────────────────────────────────
    // increment()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("increment()")
    class Increment {

        @Test
        @DisplayName("Should call Redis INCR for the click counter key")
        void shouldCallRedisIncrement() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            bufferService.increment("abc123");

            verify(valueOperations).increment("url:clicks:abc123");
        }

        @Test
        @DisplayName("Should call Redis INCR for different short links independently")
        void shouldIncrementDifferentKeysIndependently() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            bufferService.increment("link-a");
            bufferService.increment("link-b");

            verify(valueOperations).increment("url:clicks:link-a");
            verify(valueOperations).increment("url:clicks:link-b");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getBufferedCount()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getBufferedCount()")
    class GetBufferedCount {

        @Test
        @DisplayName("Should return the Redis counter value")
        void shouldReturnRedisValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("url:clicks:abc123")).thenReturn("5");

            assertThat(bufferService.getBufferedCount("abc123")).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return zero when key does not exist")
        void shouldReturnZeroForNonExistent() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("url:clicks:unknown")).thenReturn(null);

            assertThat(bufferService.getBufferedCount("unknown")).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return zero when Redis throws exception")
        void shouldReturnZeroOnRedisError() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

            assertThat(bufferService.getBufferedCount("abc123")).isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // flushToDatabase()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("flushToDatabase()")
    class FlushToDatabase {

        @Test
        @DisplayName("Should not call flushService when no click keys exist in Redis")
        void shouldNotCallFlushServiceWhenEmpty() {
            mockScanEmpty();

            bufferService.flushToDatabase();

            verifyNoInteractions(flushService);
        }

        @Test
        @DisplayName("Should flush click counts from Redis to database via Lua script")
        void shouldFlushClicksToDatabase() {
            mockScanKeys("url:clicks:abc123");
            when(redisTemplate.execute(any(), eq(Collections.singletonList("url:clicks:abc123"))))
                    .thenReturn("3");
            doNothing().when(flushService).flushSingleKey("abc123", 3);

            bufferService.flushToDatabase();

            verify(flushService).flushSingleKey("abc123", 3);
        }

        @Test
        @DisplayName("Should flush multiple short links in a single cycle")
        void shouldFlushMultipleKeys() {
            mockScanKeys("url:clicks:link-a", "url:clicks:link-b");
            when(redisTemplate.execute(any(), eq(Collections.singletonList("url:clicks:link-a"))))
                    .thenReturn("2");
            when(redisTemplate.execute(any(), eq(Collections.singletonList("url:clicks:link-b"))))
                    .thenReturn("5");
            doNothing().when(flushService).flushSingleKey(anyString(), anyLong());

            bufferService.flushToDatabase();

            verify(flushService).flushSingleKey("link-a", 2);
            verify(flushService).flushSingleKey("link-b", 5);
        }

        @Test
        @DisplayName("Should re-add delta to Redis when DB write fails (data loss fix)")
        void shouldRequeueDeltaOnDbFailure() {
            mockScanKeys("url:clicks:abc123");
            when(redisTemplate.execute(any(), eq(Collections.singletonList("url:clicks:abc123"))))
                    .thenReturn("7");
            doThrow(new RuntimeException("DB unavailable")).when(flushService).flushSingleKey("abc123", 7);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            bufferService.flushToDatabase();

            // Verify the original delta (7) is restored, not 0
            verify(valueOperations).increment("url:clicks:abc123", 7L);
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
            mockScanKeys("url:clicks:abc123");
            when(redisTemplate.execute(any(), eq(Collections.singletonList("url:clicks:abc123"))))
                    .thenReturn("2");
            doNothing().when(flushService).flushSingleKey("abc123", 2);

            bufferService.shutdown();

            verify(flushService).flushSingleKey("abc123", 2);
        }
    }
}

