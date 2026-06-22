package com.sandhya.cachingproxy.model;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
public class CacheStats {

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong cacheSize = new AtomicLong(0);

    public void recordHit() {
        hitCount.incrementAndGet();
    }

    public void recordMiss() {
        missCount.incrementAndGet();
    }

    public void incrementSize() {
        cacheSize.incrementAndGet();
    }

    public void resetSize() {
        cacheSize.set(0);
    }

    public double getHitRatio() {
        long total = hitCount.get() + missCount.get();
        if (total == 0) return 0.0;
        return (double) hitCount.get() / total;
    }

    @Override
    public String toString() {
        return String.format(
            "CacheStats{hits=%d, misses=%d, size=%d, hitRatio=%.2f%%}",
            hitCount.get(), missCount.get(), cacheSize.get(), getHitRatio() * 100
        );
    }
}