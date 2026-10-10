package com.perfumestore.backend.auth;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.perfumestore.backend.shared.api.ApiRequestException;

import org.springframework.stereotype.Component;

@Component
public class AttemptRateLimiter {

    private static final int MAX_BUCKETS = 10_000;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void check(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();
        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= windowMillis) {
                return new Bucket(now, 1);
            }
            return new Bucket(current.startedAt, current.attempts + 1);
        });
        cleanup(now, windowMillis);
        if (bucket.attempts > limit) {
            long retry = Math.max(1, (bucket.startedAt + windowMillis - now + 999) / 1000);
            throw new ApiRequestException(429, "TOO_MANY_ATTEMPTS", "Muitas tentativas",
                    "Tente novamente mais tarde.", null, retry);
        }
    }

    public void clear(String key) {
        buckets.remove(key);
    }

    private void cleanup(long now, long windowMillis) {
        if (buckets.size() <= MAX_BUCKETS) {
            return;
        }
        Iterator<Map.Entry<String, Bucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Bucket> entry = iterator.next();
            if (now - entry.getValue().startedAt >= windowMillis) {
                iterator.remove();
            }
            if (buckets.size() <= MAX_BUCKETS) {
                break;
            }
        }
    }

    private record Bucket(long startedAt, int attempts) { }
}
