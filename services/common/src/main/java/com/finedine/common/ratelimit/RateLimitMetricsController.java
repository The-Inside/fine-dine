package com.finedine.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoint for accessing rate limit metrics.
 * Available at GET /api/metrics/rate-limit
 */
@RestController
@RequestMapping("/api/metrics/rate-limit")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitMetricsController {

    private final RateLimitMetrics metrics;

    /**
     * Get all rate limit violation metrics.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalViolations", metrics.getTotalViolations());
        response.put("violations", metrics.getViolationMetrics());
        return ResponseEntity.ok(response);
    }

    /**
     * Reset rate limit metrics (for testing/debugging).
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetMetrics() {
        metrics.reset();
        return ResponseEntity.ok("Rate limit metrics reset successfully");
    }
}
