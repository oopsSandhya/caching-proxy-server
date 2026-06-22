package com.sandhya.cachingproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class CachingProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CachingProxyApplication.class, args);

        log.info("═══════════════════════════════════════════════════════");
        log.info("  Caching Proxy Server started successfully!");
        log.info("  Proxy endpoint : http://localhost:8081/proxy/**");
        log.info("  Cache stats    : http://localhost:8081/proxy/cache/stats");
        log.info("  Cache clear    : DELETE http://localhost:8081/proxy/cache");
        log.info("  Health check   : http://localhost:8081/actuator/health");
        log.info("═══════════════════════════════════════════════════════");
    }
}