package com.twekl.dashboard.config.administration;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * High-performance In-Memory Caching Configuration.
 * Caches heavy aggregate queries with self-expiring TTL to prevent database exhaustion under high traffic.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("dashboardSummary", "timeFilterPresets", "customerStats", "satisfactionDetails", "followupSummaries") {
            @Override
            protected Cache createConcurrentMapCache(String name) {
                long ttlMs = "timeFilterPresets".equals(name) ? 60_000L : 5_000L;
                return new TtlConcurrentMapCache(name, ttlMs);
            }
        };
    }

    private static class TtlConcurrentMapCache extends ConcurrentMapCache {
        private final long ttlMs;
        private final ConcurrentMap<Object, Long> timestamps = new ConcurrentHashMap<>();

        public TtlConcurrentMapCache(String name, long ttlMs) {
            super(name);
            this.ttlMs = ttlMs;
        }

        @Override
        public ValueWrapper get(Object key) {
            Long ts = timestamps.get(key);
            if (ts != null && (System.currentTimeMillis() - ts) > ttlMs) {
                evict(key);
                return null;
            }
            return super.get(key);
        }

        @Override
        public void put(Object key, Object value) {
            timestamps.put(key, System.currentTimeMillis());
            super.put(key, value);
        }

        @Override
        public void evict(Object key) {
            timestamps.remove(key);
            super.evict(key);
        }

        @Override
        public void clear() {
            timestamps.clear();
            super.clear();
        }
    }
}
