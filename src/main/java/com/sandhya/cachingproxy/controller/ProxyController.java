package com.sandhya.cachingproxy.controller;

import com.sandhya.cachingproxy.model.CachedResponse;
import com.sandhya.cachingproxy.service.CacheService;
import com.sandhya.cachingproxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class ProxyController {

    private final CacheService cacheService;
    private final ProxyService proxyService;

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body
    ) {
        String method      = request.getMethod();
        String path        = extractPath(request);
        String queryString = request.getQueryString();
        HttpHeaders headers = extractHeaders(request);

        String targetUrl = proxyService.buildTargetUrl(path, queryString);
        String cacheKey  = cacheService.buildKey(method, targetUrl);

        log.info("─── Incoming: {} {} → CacheKey: {}", method, path, cacheKey);

        if ("GET".equalsIgnoreCase(method)) {
            Optional<CachedResponse> cached = cacheService.get(cacheKey);

            if (cached.isPresent()) {
                log.info("✅ X-Cache: HIT for {}", cacheKey);
                return buildResponse(cached.get(), "HIT");
            }
        }

        log.info("❌ X-Cache: MISS for {} — forwarding to origin", cacheKey);
        CachedResponse freshResponse = proxyService.forward(method, path, queryString, headers, body);

        boolean isGet        = "GET".equalsIgnoreCase(method);
        boolean isSuccessful = freshResponse.getStatus().is2xxSuccessful();

        if (isGet && isSuccessful) {
            cacheService.put(cacheKey, freshResponse);
            log.info("💾 Cached response for key: {}", cacheKey);
        }

        return buildResponse(freshResponse, "MISS");
    }

    private ResponseEntity<String> buildResponse(CachedResponse cachedResponse, String cacheStatus) {
        HttpHeaders responseHeaders = new HttpHeaders();

        if (cachedResponse.getHeaders() != null) {
            cachedResponse.getHeaders().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("Transfer-Encoding")
                        && !name.equalsIgnoreCase("Connection")) {
                    responseHeaders.addAll(name, values);
                }
            });
        }

        responseHeaders.add("X-Cache", cacheStatus);
        responseHeaders.add("X-Proxy-By", "CachingProxy/1.0");

        return new ResponseEntity<>(
                cachedResponse.getBody(),
                responseHeaders,
                cachedResponse.getStatus()
        );
    }

    private String extractPath(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String stripped = fullPath.replaceFirst("/proxy", "");
        return stripped.isBlank() ? "/" : stripped;
    }

   private HttpHeaders extractHeaders(HttpServletRequest request) {
    HttpHeaders headers = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();

    if (headerNames != null) {
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            // Skip Accept-Encoding 
            if (!name.equalsIgnoreCase("Accept-Encoding")) {
                headers.add(name, request.getHeader(name));
            }
        }
    }

    return headers;
}
}