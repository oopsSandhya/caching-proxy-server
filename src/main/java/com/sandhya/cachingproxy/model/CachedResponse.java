package com.sandhya.cachingproxy.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Getter
@Builder
public class CachedResponse {

    private final String body;
    private final HttpStatus status;
    private final HttpHeaders headers;
    private final Instant cachedAt;

    public boolean isExpired(long ttlSeconds) {
        return Instant.now().isAfter(cachedAt.plusSeconds(ttlSeconds));
    }
}