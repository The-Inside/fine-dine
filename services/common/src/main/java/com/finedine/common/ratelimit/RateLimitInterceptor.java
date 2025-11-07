package com.finedine.common.ratelimit;

import com.finedine.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        String clientIp = request.getRemoteAddr();
        String endpoint = request.getRequestURI();
        String key = "rate_limit:" + clientIp + ":" + endpoint;

        long requests = redisTemplate.opsForValue().increment(key, 1);

        if (requests == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(rateLimit.windowSeconds()));
        }

        if (requests > rateLimit.limit()) {
            log.error("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, endpoint);
            log.error("try again in "+ rateLimit.windowSeconds()+ " seconds");
            throw new RateLimitExceededException("requests limit exceeded, try again later");
        }

        return true;
    }
}