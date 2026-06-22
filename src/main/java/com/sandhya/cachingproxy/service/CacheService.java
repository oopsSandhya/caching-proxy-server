package com.sandhya.cachingproxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandhya.cachingproxy.model.CachedResponse;
import com.sandhya.cachingproxy.model.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CacheStats stats = new CacheStats();

    @Value("${proxy.cache.ttl.seconds:60}")
    private long ttlSeconds;

    private static final String KEY_PREFIX = "proxy:cache:";

    public Optional<CachedResponse> get(String key) {
        try {
            String redisKey = KEY_PREFIX + key;
            String cached = redisTemplate.opsForValue().get(redisKey);

            if (cached == null) {
                stats.recordMiss();
                log.debug("Cache MISS for key: {}", key);
                return Optional.empty();
            }

            stats.recordHit();
            log.debug("Cache HIT for key: {}", key);
            CachedResponse response = deserialize(cached);
            return Optional.ofNullable(response);

        } catch (Exception ex) {
            log.warn("Redis get failed for key: {} | Error: {}", key, ex.getMessage());
            stats.recordMiss();
            return Optional.empty();
        }
    }

    public void put(String key, CachedResponse response) {
        try {
            String redisKey = KEY_PREFIX + key;
            String serialized = serialize(response);
            redisTemplate.opsForValue().set(redisKey, serialized, ttlSeconds, TimeUnit.SECONDS);
            stats.incrementSize();
            log.debug("Cache STORE for key: {} | TTL={}s", key, ttlSeconds);
        } catch (Exception ex) {
            log.warn("Redis put failed for key: {} | Error: {}", key, ex.getMessage());
        }
    }

    public void clearCache() {
        try {
            var keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cache CLEARED — {} entries removed", keys.size());
            }
            stats.resetSize();
        } catch (Exception ex) {
            log.warn("Redis clear failed | Error: {}", ex.getMessage());
        }
    }

    public CacheStats getStats() {
        return stats;
    }

    public int getCacheSize() {
        try {
            var keys = redisTemplate.keys(KEY_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception ex) {
            return 0;
        }
    }

    public String buildKey(String method, String url) {
        return method.toUpperCase() + ":" + url;
    }

    @Scheduled(fixedDelay = 30000)
    public void evictExpiredEntries() {
        log.info("Scheduled cache cleanup running — Redis handles TTL automatically");
    }

    private String serialize(CachedResponse response) throws Exception {
        return objectMapper.writeValueAsString(new CacheEntry(
                response.getBody(),
                response.getStatus().value(),
                response.getHeaders().toSingleValueMap(),
                response.getCachedAt().toString()
        ));
    }

    private CachedResponse deserialize(String json) throws Exception {
        CacheEntry entry = objectMapper.readValue(json, CacheEntry.class);
        HttpHeaders headers = new HttpHeaders();
        entry.headers().forEach(headers::add);

        return CachedResponse.builder()
                .body(entry.body())
                .status(HttpStatus.valueOf(entry.statusCode()))
                .headers(headers)
                .cachedAt(Instant.parse(entry.cachedAt()))
                .build();
    }

    private record CacheEntry(
            String body,
            int statusCode,
            java.util.Map<String, String> headers,
            String cachedAt
    ) {}
}