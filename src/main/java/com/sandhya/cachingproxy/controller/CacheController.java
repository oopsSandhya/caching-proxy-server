package com.sandhya.cachingproxy.controller;

import com.sandhya.cachingproxy.model.CacheStats;
import com.sandhya.cachingproxy.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/proxy/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCache() {
        log.info("Cache clear requested via API");
        cacheService.clearCache();

        return ResponseEntity.ok(Map.of(
                "status",  "success",
                "message", "Cache cleared successfully",
                "entries", "0"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        CacheStats stats = cacheService.getStats();

        Map<String, Object> response = Map.of(
                "hitCount",  stats.getHitCount().get(),
                "missCount", stats.getMissCount().get(),
                "cacheSize", cacheService.getCacheSize(),
                "hitRatio",  String.format("%.2f%%", stats.getHitRatio() * 100),
                "summary",   stats.toString()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        int size = cacheService.getCacheSize();

        Map<String, Object> health = Map.of(
                "status",    "UP",
                "cacheSize", size,
                "cacheType", "ConcurrentHashMap (In-Memory)",
                "message",   size > 0
                        ? size + " entries cached"
                        : "Cache is empty"
        );

        return ResponseEntity.ok(health);
    }
}