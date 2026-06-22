
package com.sandhya.cachingproxy;

import com.sandhya.cachingproxy.model.CachedResponse;
import com.sandhya.cachingproxy.model.CacheStats;
import com.sandhya.cachingproxy.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cacheService, "ttlSeconds", 60L);
    }

    // ── Test 1: Cache MISS ────────────────────────────────────────────────────
    @Test
    void testCacheMiss_WhenKeyNotInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<CachedResponse> result = cacheService.get("GET:https://example.com/posts/1");

        assertFalse(result.isPresent());
        assertEquals(1, cacheService.getStats().getMissCount().get());
        assertEquals(0, cacheService.getStats().getHitCount().get());
    }

    // ── Test 2: Cache HIT ─────────────────────────────────────────────────────
    @Test
    void testCacheHit_WhenKeyExistsInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String serialized = "{\"body\":\"{\\\"id\\\":1}\",\"statusCode\":200,"
                + "\"headers\":{},\"cachedAt\":\"" + Instant.now() + "\"}";
        when(valueOperations.get(anyString())).thenReturn(serialized);

        Optional<CachedResponse> result = cacheService.get("GET:https://example.com/posts/1");

        assertTrue(result.isPresent());
        assertEquals(1, cacheService.getStats().getHitCount().get());
        assertEquals(0, cacheService.getStats().getMissCount().get());
    }

    // ── Test 3: Cache Key Format ──────────────────────────────────────────────
    @Test
    void testBuildKey_ReturnsCorrectFormat() {
        String key = cacheService.buildKey("GET", "https://example.com/posts/1");
        assertEquals("GET:https://example.com/posts/1", key);
    }

    // ── Test 4: Cache Key Uppercase ───────────────────────────────────────────
    @Test
    void testBuildKey_UppercasesMethod() {
        String key = cacheService.buildKey("get", "https://example.com/posts/1");
        assertEquals("GET:https://example.com/posts/1", key);
    }

    // ── Test 5: Cache Clear ───────────────────────────────────────────────────
    @Test
    void testClearCache_DeletesAllKeys() {
        when(redisTemplate.keys(anyString()))
                .thenReturn(Set.of("proxy:cache:key1", "proxy:cache:key2"));

        cacheService.clearCache();

        verify(redisTemplate, times(1)).delete(anyCollection());
    }

    // ── Test 6: Hit Ratio ─────────────────────────────────────────────────────
    @Test
    void testHitRatio_CalculatesCorrectly() {
        CacheStats stats = cacheService.getStats();
        stats.recordHit();
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();

        assertEquals(0.75, stats.getHitRatio(), 0.001);
    }

    // ── Test 7: Initial Stats ─────────────────────────────────────────────────
    @Test
    void testStats_InitialStateIsZero() {
        CacheStats stats = cacheService.getStats();
        assertEquals(0, stats.getHitCount().get());
        assertEquals(0, stats.getMissCount().get());
        assertEquals(0.0, stats.getHitRatio());
    }
}