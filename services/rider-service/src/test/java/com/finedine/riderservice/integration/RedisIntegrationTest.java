package com.finedine.riderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Redis geospatial operations
 * Uses embedded Redis server for testing
 */
class RedisIntegrationTest {

    private static RedisServer redisServer;
    private RedisTemplate<String, Object> redisTemplate;
    private RedisConnectionFactory connectionFactory;

    private static final String LOCATIONS_KEY = "test:locations:all";
    private static final String RIDER_PREFIX = "rider:";
    private static final String DELIVERY_PREFIX = "delivery:";

    @BeforeAll
    static void setUpClass() throws IOException {
        // Start embedded Redis on port 6370 (different from default 6379)
        redisServer = new RedisServer(6370);
        redisServer.start();
    }

    @AfterAll
    static void tearDownClass() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        // Configure Redis connection
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6370);

        connectionFactory = new LettuceConnectionFactory(config);
        ((LettuceConnectionFactory) connectionFactory).afterPropertiesSet();

        // Configure RedisTemplate
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();

        // Clean up any existing test data
        redisTemplate.delete(LOCATIONS_KEY);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        redisTemplate.delete(LOCATIONS_KEY);

        // Close connection factory
        if (connectionFactory instanceof LettuceConnectionFactory) {
            ((LettuceConnectionFactory) connectionFactory).destroy();
        }
    }

    @Test
    void testGeoadd_AddRiderLocation_Success() {
        // Given
        Long riderId = 1L;
        double latitude = 40.7128;  // New York
        double longitude = -74.0060;

        // When
        Long result = redisTemplate.opsForGeo().add(
            LOCATIONS_KEY,
            new Point(longitude, latitude),
            RIDER_PREFIX + riderId  // e.g., "rider:1"
        );

        // Then
        assertNotNull(result);
        assertEquals(1L, result); // 1 member added

        // Verify location was stored
        List<Point> positions = redisTemplate.opsForGeo().position(
            LOCATIONS_KEY,
            RIDER_PREFIX + riderId
        );
        assertNotNull(positions);
        assertFalse(positions.isEmpty());
        assertEquals(longitude, positions.get(0).getX(), 0.001);
        assertEquals(latitude, positions.get(0).getY(), 0.001);
    }

    @Test
    void testGeodist_CalculateDistance_ReturnsCorrectValue() {
        // Given - Rider and delivery in same key with prefixes
        // Times Square: 40.7580° N, 73.9855° W
        // Central Park South: 40.7678° N, 73.9812° W
        Long riderId = 1L;
        Long deliveryId = 100L;

        // Add rider location
        redisTemplate.opsForGeo().add(
            LOCATIONS_KEY,
            new Point(-73.9855, 40.7580),
            RIDER_PREFIX + riderId  // "rider:1"
        );

        // Add delivery location
        redisTemplate.opsForGeo().add(
            LOCATIONS_KEY,
            new Point(-73.9812, 40.7678),
            DELIVERY_PREFIX + deliveryId  // "delivery:100"
        );

        // When - Calculate distance using GEODIST (NO Haversine!)
        Distance distance = redisTemplate.opsForGeo().distance(
            LOCATIONS_KEY,
            RIDER_PREFIX + riderId,
            DELIVERY_PREFIX + deliveryId,
            Metrics.KILOMETERS
        );

        // Then
        assertNotNull(distance);
        // Distance between these two points is approximately 1.1 km
        assertTrue(distance.getValue() > 1.0);
        assertTrue(distance.getValue() < 1.3);
        assertEquals(Metrics.KILOMETERS, distance.getMetric());
    }

    @Test
    void testGeodist_BetweenDifferentKeys_UsingPositions() {
        // Given - Rider in one key, customer in another
        Long riderId = 1L;
        Long deliveryId = 100L;

        // Rider location (Times Square)
        redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-73.9855, 40.7580),
            riderId.toString()
        );

        // Customer location (Central Park)
        redisTemplate.opsForGeo().add(
            CUSTOMER_LOCATION_KEY,
            new Point(-73.9812, 40.7678),
            deliveryId.toString()
        );

        // When - Get positions
        List<Point> riderPositions = redisTemplate.opsForGeo().position(
            RIDER_LOCATION_KEY,
            riderId.toString()
        );

        List<Point> customerPositions = redisTemplate.opsForGeo().position(
            CUSTOMER_LOCATION_KEY,
            deliveryId.toString()
        );

        // Then
        assertNotNull(riderPositions);
        assertNotNull(customerPositions);
        assertFalse(riderPositions.isEmpty());
        assertFalse(customerPositions.isEmpty());

        Point riderPoint = riderPositions.get(0);
        Point customerPoint = customerPositions.get(0);

        // Calculate distance using Haversine
        double distance = calculateHaversineDistance(
            riderPoint.getY(), riderPoint.getX(),
            customerPoint.getY(), customerPoint.getX()
        );

        // Verify distance is approximately 1.1 km
        assertTrue(distance > 1.0);
        assertTrue(distance < 1.3);
    }

    @Test
    void testTTL_ExpiredKey_ReturnsNull() throws InterruptedException {
        // Given
        Long riderId = 1L;
        redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-73.9855, 40.7580),
            riderId.toString()
        );

        // Set TTL of 1 second
        redisTemplate.expire(RIDER_LOCATION_KEY, 1, TimeUnit.SECONDS);

        // Verify key exists
        List<Point> positionsBefore = redisTemplate.opsForGeo().position(
            RIDER_LOCATION_KEY,
            riderId.toString()
        );
        assertNotNull(positionsBefore);
        assertFalse(positionsBefore.isEmpty());

        // Wait for expiration
        Thread.sleep(1500);

        // When - Try to get position after expiration
        List<Point> positionsAfter = redisTemplate.opsForGeo().position(
            RIDER_LOCATION_KEY,
            riderId.toString()
        );

        // Then - Key should be expired
        assertTrue(positionsAfter == null || positionsAfter.isEmpty() || positionsAfter.get(0) == null);
    }

    @Test
    void testGeoadd_UpdateExistingLocation_Succeeds() {
        // Given
        Long riderId = 1L;

        // Add initial location
        redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-73.9855, 40.7580),
            riderId.toString()
        );

        // When - Update to new location
        Long result = redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-73.9812, 40.7678),
            riderId.toString()
        );

        // Then
        assertEquals(0L, result); // 0 because member already exists (updated)

        // Verify new location
        List<Point> positions = redisTemplate.opsForGeo().position(
            RIDER_LOCATION_KEY,
            riderId.toString()
        );
        assertNotNull(positions);
        assertEquals(-73.9812, positions.get(0).getX(), 0.001);
        assertEquals(40.7678, positions.get(0).getY(), 0.001);
    }

    @Test
    void testGeodist_NonExistentMember_ReturnsNull() {
        // Given
        Long riderId = 1L;
        Long deliveryId = 999L; // Non-existent

        redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-73.9855, 40.7580),
            riderId.toString()
        );

        // When
        Distance distance = redisTemplate.opsForGeo().distance(
            RIDER_LOCATION_KEY,
            riderId.toString(),
            deliveryId.toString(),
            Metrics.KILOMETERS
        );

        // Then
        assertNull(distance);
    }

    @Test
    void testGeoadd_MultipleLocations_AllStored() {
        // Given
        redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-73.9855, 40.7580),
            "1"
        );
        redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-73.9812, 40.7678),
            "2"
        );
        redisTemplate.opsForGeo().add(
            RIDER_LOCATION_KEY,
            new Point(-74.0060, 40.7128),
            "3"
        );

        // When - Calculate distances
        Distance dist1to2 = redisTemplate.opsForGeo().distance(
            RIDER_LOCATION_KEY,
            "1", "2",
            Metrics.KILOMETERS
        );

        Distance dist1to3 = redisTemplate.opsForGeo().distance(
            RIDER_LOCATION_KEY,
            "1", "3",
            Metrics.KILOMETERS
        );

        // Then
        assertNotNull(dist1to2);
        assertNotNull(dist1to3);
        assertTrue(dist1to2.getValue() > 0);
        assertTrue(dist1to3.getValue() > 0);
        // dist1to3 should be greater than dist1to2
        assertTrue(dist1to3.getValue() > dist1to2.getValue());
    }

    @Test
    void testRedisConnection_HealthCheck() {
        // When
        String response = redisTemplate.getConnectionFactory()
            .getConnection()
            .ping();

        // Then
        assertEquals("PONG", response);
    }

    /**
     * Helper method to calculate Haversine distance between two coordinates
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
