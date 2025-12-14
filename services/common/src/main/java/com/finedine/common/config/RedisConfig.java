package com.finedine.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration for rate limiting.
 * Spring Data Redis is automatically configured via application.yml.
 * Rate limiting uses StringRedisTemplate with Lua scripting for atomic operations.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    public RedisConfig() {
        log.info("Redis configuration loaded for rate limiting");
    }
}