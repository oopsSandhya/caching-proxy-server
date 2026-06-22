package com.sandhya.cachingproxy.service;

import com.sandhya.cachingproxy.model.CachedResponse;
import com.sandhya.cachingproxy.model.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CacheService {

    private final ConcurrentHashMap<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private final CacheStats stats = new CacheStats();

    @Value("${proxy.cache.ttl.seconds:60}")
    private long ttlSeconds;

    public Optional<CachedResponse> get(String key) {
        CachedResponse cached = cache.get(key);

        if (cached == null) {
            stats.recordMiss();
            log.debug("Cache MISS for key: {}", key);
            return Optional.empty();
        }

        if (cached.isExpired(ttlSeconds)) {
            log.debug("Cache EXPIRED for key: {}", key);
            cache.remove(key);
            stats.resetSize();
            stats.recordMiss();
            return Optional.empty();
        }

        stats.recordHit();
        log.debug("Cache HIT for key: {}", key);
        return Optional.of(cached);
    }

    public void put(String key, CachedResponse response) {
        cache.put(key, response);
        stats.incrementSize();
        log.debug("Cache STORE for key: {}", key);
    }

    public void clearCache() {
        int size = cache.size();
        cache.clear();
        stats.resetSize();
        log.info("Cache CLEARED — {} entries removed", size);
    }

    public CacheStats getStats() {
        return stats;
    }

    public int getCacheSize() {
        return cache.size();
    }

    public String buildKey(String method, String url) {
        return method.toUpperCase() + ":" + url;
    }

    // ── Scheduled TTL Eviction ────────────────────────────────────────────

    @Scheduled(fixedDelay = 30000)
    public void evictExpiredEntries() {
        log.info("Running scheduled cache cleanup...");
        int before = cache.size();

        cache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(ttlSeconds);
            if (expired) {
                log.debug("Evicting expired key: {}", entry.getKey());
            }
            return expired;
        });

        int after = cache.size();
        int removed = before - after;

        if (removed > 0) {
            log.info("Evicted {} expired entries. Cache size: {}", removed, after);
        } else {
            log.info("No expired entries found. Cache size: {}", after);
        }
    }
}