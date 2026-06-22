package com.sandhya.cachingproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Slf4j
@SpringBootApplication

public class CachingProxyApplication {
     public static void main(String[] args) {
        SpringApplication.run(CachingProxyApplication.class, args);

        log.info("═══════════════════════════════════════════════════════");
        log.info("  Caching Proxy Server started successfully! 🚀");
        log.info("  Proxy endpoint : http://localhost:8080/proxy/**");
        log.info("  Cache stats    : http://localhost:8080/proxy/cache/stats");
        log.info("  Cache clear    : DELETE http://localhost:8080/proxy/cache");
        log.info("  Health check   : http://localhost:8080/actuator/health");
        log.info("═══════════════════════════════════════════════════════");
    }
}
    

