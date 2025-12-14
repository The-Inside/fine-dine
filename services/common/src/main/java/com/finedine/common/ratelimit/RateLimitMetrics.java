package com.finedine.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks rate limit violation metrics for monitoring and observability.
 * Provides insights into which endpoints and IPs are hitting rate limits.
 */
@Slf4j
@Component
public class RateLimitMetrics {

    private final Map<String, AtomicLong> violationCountByKey = new ConcurrentHashMap<>();
    private final AtomicLong totalViolations = new AtomicLong(0);

    /**
     * Records a rate limit violation for a given key (clientIp:endpoint).
     */
    public void recordViolation(String clientIp, String endpoint) {
        String key = clientIp + ":" + endpoint;
        violationCountByKey.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        totalViolations.incrementAndGet();

        long count = violationCountByKey.get(key).get();

        // Log at WARN level every 10th violation for the same endpoint/IP pair
        if (count % 10 == 0) {
            log.warn("Rate limit violations summary - IP: {}, Endpoint: {}, Violations: {}, Total: {}",
                    clientIp, endpoint, count, totalViolations.get());
        }
    }

    /**
     * Get violation count for a specific IP:endpoint pair.
     */
    public long getViolationCount(String clientIp, String endpoint) {
        String key = clientIp + ":" + endpoint;
        AtomicLong count = violationCountByKey.get(key);
        return count != null ? count.get() : 0L;
    }

    /**
     * Get total violations across all endpoints and IPs.
     */
    public long getTotalViolations() {
        return totalViolations.get();
    }

    /**
     * Get violation metrics as a map for monitoring.
     */
    public Map<String, Long> getViolationMetrics() {
        Map<String, Long> metrics = new ConcurrentHashMap<>();
        violationCountByKey.forEach((key, count) -> metrics.put(key, count.get()));
        return metrics;
    }

    /**
     * Reset all metrics (useful for testing).
     */
    public void reset() {
        violationCountByKey.clear();
        totalViolations.set(0);
        log.info("Rate limit metrics reset");
    }
}
