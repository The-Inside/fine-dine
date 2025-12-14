package com.finedine.common.ratelimit;

import com.finedine.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitMetrics metrics;

    // Lua script for atomic increment and expire
    private static final String RATE_LIMIT_SCRIPT =
            "local current = redis.call('incr', KEYS[1]) " +
                    "if current == 1 then " +
                    "    redis.call('expire', KEYS[1], ARGV[1]) " +
                    "end " +
                    "return current";

    // Default global rate limit: 100 requests per 60 seconds
    private static final int DEFAULT_LIMIT = 100;
    private static final long DEFAULT_WINDOW_SECONDS = 60L;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate, RateLimitMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        // Use explicit annotation or apply default global rate limit
        int limit = DEFAULT_LIMIT;
        long windowSeconds = DEFAULT_WINDOW_SECONDS;

        if (rateLimit != null) {
            limit = rateLimit.limit();
            windowSeconds = rateLimit.windowSeconds();
        }

        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();
        String key = "rate_limit:" + clientIp + ":" + endpoint;

        try {
            // Execute Lua script atomically
            Long requests = redisTemplate.execute(
                    RedisScript.of(RATE_LIMIT_SCRIPT, Long.class),
                    Collections.singletonList(key),
                    String.valueOf(windowSeconds)
            );

            if (requests != null && requests > limit) {
                metrics.recordViolation(clientIp, endpoint);
                log.warn("Rate limit exceeded for IP: {} on endpoint: {}. Limit: {}/{} seconds. Request count: {}",
                        clientIp, endpoint, limit, windowSeconds, requests);
                throw new RateLimitExceededException("requests limit exceeded, try again later");
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis rate limiting failed for IP: {} on endpoint: {}. Allowing request to proceed.",
                    clientIp, endpoint, e);
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (used by most proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // Take the first one (leftmost) as the original client IP
            String clientIp = xForwardedFor.split(",")[0].trim();
            if (isValidIp(clientIp)) {
                return clientIp;
            }
        }

        // Check X-Real-IP header (used by Nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && isValidIp(xRealIp)) {
            return xRealIp;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        try {
            java.net.InetAddress.getByName(ip);
            return true;
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }
}