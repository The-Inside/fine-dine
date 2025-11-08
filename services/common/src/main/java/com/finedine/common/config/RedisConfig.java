package com.finedine.common.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Redis configuration for Bucket4j rate limiting.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    /**
     * Creates a Bucket4j ProxyManager for Redis-based distributed rate limiting.
     * Uses Lettuce as the Redis client.
     *
     * @param connectionFactory Spring's Redis connection factory
     * @return ProxyManager instance for Bucket4j
     */
    @Bean
    public ProxyManager<String> bucket4jProxyManager(RedisConnectionFactory connectionFactory) {
        log.info("Initializing Bucket4j Redis ProxyManager");

        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;

        // Build Redis URI from connection factory settings
        String redisUri = buildRedisUri(lettuceFactory);
        log.info("Connecting to Redis for rate limiting: {}", maskPassword(redisUri));

        // Create Lettuce Redis client
        RedisClient redisClient = RedisClient.create(redisUri);

        // Create connection with String keys and byte[] values
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(codec);

        // Create Bucket4j proxy manager
        ProxyManager<String> proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();

        log.info("Bucket4j Redis ProxyManager initialized successfully");
        return proxyManager;
    }

    /**
     * Builds Redis URI from LettuceConnectionFactory settings.
     */
    private String buildRedisUri(LettuceConnectionFactory factory) {
        String host = factory.getHostName();
        int port = factory.getPort();
        String password = factory.getPassword();
        boolean useSsl = factory.isUseSsl();

        StringBuilder uri = new StringBuilder();
        uri.append(useSsl ? "rediss://" : "redis://");

        if (password != null && !password.isEmpty()) {
            uri.append(":").append(password).append("@");
        }

        uri.append(host).append(":").append(port);

        return uri.toString();
    }

    /**
     * Masks password in Redis URI for logging.
     */
    private String maskPassword(String uri) {
        return uri.replaceAll(":[^@]+@", ":****@");
    }
}