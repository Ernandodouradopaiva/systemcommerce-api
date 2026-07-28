package br.com.systemcommerce.dashboard.executive.support;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cache controlado em memória para dashboard executivo (Prompt 87).
 * TTL configurável; invalidação por chave.
 */
@Component
public class ExecutiveDashboardCache {

    private final ConcurrentHashMap<String, Entry<?>> store = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public ExecutiveDashboardCache(
            @Value("${systemcommerce.dashboard.executive.cache-ttl-seconds:120}") long ttlSeconds) {
        this.ttlSeconds = Math.max(30, ttlSeconds);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Entry<?> entry = store.get(key);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of((T) entry.value);
    }

    public <T> void put(String key, T value) {
        store.put(key, new Entry<>(value, Instant.now().plusSeconds(ttlSeconds)));
    }

    public void invalidateAll() {
        store.clear();
    }

    private record Entry<T>(T value, Instant expiresAt) {}
}
