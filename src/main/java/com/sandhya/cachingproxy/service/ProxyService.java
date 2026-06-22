package com.sandhya.cachingproxy.service;

import com.sandhya.cachingproxy.model.CachedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final RestTemplate restTemplate;

    @Value("${proxy.origin.url}")
    private String originUrl;

    public CachedResponse forward(
            String method,
            String path,
            String queryString,
            HttpHeaders requestHeaders,
            String body
    ) {
        String targetUrl = buildTargetUrl(path, queryString);
        log.info("Forwarding {} {} → {}", method, path, targetUrl);

        HttpHeaders forwardHeaders = buildForwardHeaders(requestHeaders);
        HttpEntity<String> entity = new HttpEntity<>(body, forwardHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.valueOf(method),
                    entity,
                    String.class
            );

            log.info("Origin responded: {} for {}", response.getStatusCode(), targetUrl);

            return CachedResponse.builder()
                    .body(response.getBody())
                    .status((HttpStatus) response.getStatusCode())
                    .headers(response.getHeaders())
                    .cachedAt(Instant.now())
                    .build();

        } catch (HttpStatusCodeException ex) {
            log.warn("Origin returned error: {} for {}", ex.getStatusCode(), targetUrl);

            return CachedResponse.builder()
                    .body(ex.getResponseBodyAsString())
                    .status((HttpStatus) ex.getStatusCode())
                    .headers(ex.getResponseHeaders() != null
                            ? ex.getResponseHeaders()
                            : new HttpHeaders())
                    .cachedAt(Instant.now())
                    .build();

        } catch (Exception ex) {
            log.error("Failed to reach origin server: {}", ex.getMessage());

            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);

            return CachedResponse.builder()
                    .body("{\"error\": \"Origin server unreachable\", \"message\": \"" + ex.getMessage() + "\"}")
                    .status(HttpStatus.BAD_GATEWAY)
                    .headers(errorHeaders)
                    .cachedAt(Instant.now())
                    .build();
        }
    }

    public String buildTargetUrl(String path, String queryString) {
        String base = originUrl.endsWith("/")
                ? originUrl.substring(0, originUrl.length() - 1)
                : originUrl;

        String url = base + path;

        if (queryString != null && !queryString.isBlank()) {
            url += "?" + queryString;
        }

        return url;
    }

    private HttpHeaders buildForwardHeaders(HttpHeaders incomingHeaders) {
        HttpHeaders forwardHeaders = new HttpHeaders();

        List<String> headersToForward = List.of(
                "Content-Type",
                "Accept",
                "Authorization",
                "Accept-Language"
        );

        for (String headerName : headersToForward) {
            if (incomingHeaders.containsKey(headerName)) {
                forwardHeaders.put(headerName, incomingHeaders.get(headerName));
            }
        }

        return forwardHeaders;
    }
}