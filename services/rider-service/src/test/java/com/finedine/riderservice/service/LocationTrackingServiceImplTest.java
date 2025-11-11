package com.finedine.riderservice.service;

import com.finedine.riderservice.dto.GeofenceAlert;
import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.enums.DeliveryStatus;
import com.finedine.riderservice.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationTrackingServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private GeoOperations<String, Object> geoOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private LocationTrackingServiceImpl locationTrackingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void testAddRiderLocation_Success() {
        // Given
        Long riderId = 1L;
        double latitude = 40.7128;
        double longitude = -74.0060;

        when(geoOperations.add(anyString(), any(RedisGeoCommands.GeoLocation.class)))
            .thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any()))
            .thenReturn(true);

        // When
        locationTrackingService.addRiderLocation(riderId, latitude, longitude);

        // Then
        verify(geoOperations).add(eq("rider:locations"), any(RedisGeoCommands.GeoLocation.class));
        verify(redisTemplate).expire(eq("rider:locations"), anyLong(), any());
    }

    @Test
    void testAddCustomerLocation_Success() {
        // Given
        Long deliveryId = 100L;
        double latitude = 40.7580;
        double longitude = -73.9855;

        when(geoOperations.add(anyString(), any(RedisGeoCommands.GeoLocation.class)))
            .thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any()))
            .thenReturn(true);

        // When
        locationTrackingService.addCustomerLocation(deliveryId, latitude, longitude);

        // Then
        verify(geoOperations).add(eq("customer:locations"), any(RedisGeoCommands.GeoLocation.class));
        verify(redisTemplate).expire(eq("customer:locations"), anyLong(), any());
    }

    @Test
    void testCalculateDistance_ReturnsCorrectDistance() {
        // Given
        Long riderId = 1L;
        Long deliveryId = 100L;
        Distance expectedDistance = new Distance(5.2, Metrics.KILOMETERS);

        when(geoOperations.distance(
            eq("rider:locations"),
            eq(riderId.toString()),
            eq(deliveryId.toString()),
            eq(Metrics.KILOMETERS)
        )).thenReturn(expectedDistance);

        // When
        Double result = locationTrackingService.calculateDistance(riderId, deliveryId);

        // Then
        assertNotNull(result);
        assertEquals(5.2, result, 0.01);
        verify(geoOperations).distance(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testCalculateDistance_ReturnsNull_WhenRedisReturnsNull() {
        // Given
        Long riderId = 1L;
        Long deliveryId = 100L;

        when(geoOperations.distance(anyString(), anyString(), anyString(), any()))
            .thenReturn(null);

        // When - also need to mock position calls for fallback
        when(geoOperations.position(anyString(), anyString()))
            .thenReturn(null);

        Double result = locationTrackingService.calculateDistance(riderId, deliveryId);

        // Then - will be null because fallback also returns null
        assertNull(result);
    }

    @Test
    void testCalculateETA_ValidDistance_ReturnsCorrectETA() {
        // Given
        Long riderId = 1L;
        Long deliveryId = 100L;
        Distance distance = new Distance(15.0, Metrics.KILOMETERS); // 15 km

        when(geoOperations.distance(anyString(), anyString(), anyString(), any()))
            .thenReturn(distance);

        // When
        Double etaMinutes = locationTrackingService.calculateETA(riderId, deliveryId);

        // Then
        assertNotNull(etaMinutes);
        // ETA = (15 km / 30 km/h) * 60 = 30 minutes
        assertEquals(30.0, etaMinutes, 0.1);
    }

    @Test
    void testCalculateETA_NullDistance_ReturnsNull() {
        // Given
        Long riderId = 1L;
        Long deliveryId = 100L;

        when(geoOperations.distance(anyString(), anyString(), anyString(), any()))
            .thenReturn(null);
        when(geoOperations.position(anyString(), anyString()))
            .thenReturn(null);

        // When
        Double etaMinutes = locationTrackingService.calculateETA(riderId, deliveryId);

        // Then
        assertNull(etaMinutes);
    }

    @Test
    void testCheckGeofenceAlert_CrossingThreshold_ReturnsAlert() {
        // Given
        Long deliveryId = 100L;
        double currentDistanceKm = 0.9; // Within 1km threshold

        when(setOperations.isMember(anyString(), eq("2KM_AWAY")))
            .thenReturn(false);
        when(setOperations.isMember(anyString(), eq("1KM_AWAY")))
            .thenReturn(false);

        // When
        Optional<GeofenceAlert> alert = locationTrackingService.checkGeofenceAlert(
            deliveryId,
            currentDistanceKm
        );

        // Then
        assertTrue(alert.isPresent());
        assertEquals("1KM_AWAY", alert.get().alertType());
        assertEquals(0.9, alert.get().distanceKm(), 0.01);
        assertTrue(alert.get().message().contains("1km"));
    }

    @Test
    void testCheckGeofenceAlert_BelowThreshold_NoAlert() {
        // Given
        Long deliveryId = 100L;
        double currentDistanceKm = 5.0; // Beyond all thresholds

        // When
        Optional<GeofenceAlert> alert = locationTrackingService.checkGeofenceAlert(
            deliveryId,
            currentDistanceKm
        );

        // Then
        assertTrue(alert.isEmpty());
    }

    @Test
    void testCheckGeofenceAlert_AlreadySent_NoAlert() {
        // Given
        Long deliveryId = 100L;
        double currentDistanceKm = 0.9; // Within 1km threshold

        when(setOperations.isMember(anyString(), eq("2KM_AWAY")))
            .thenReturn(false);
        when(setOperations.isMember(anyString(), eq("1KM_AWAY")))
            .thenReturn(true); // Already sent

        // When
        Optional<GeofenceAlert> alert = locationTrackingService.checkGeofenceAlert(
            deliveryId,
            currentDistanceKm
        );

        // Then
        assertTrue(alert.isEmpty());
    }

    @Test
    void testMarkAlertSent_Success() {
        // Given
        Long deliveryId = 100L;
        String alertType = "1KM_AWAY";

        when(setOperations.add(anyString(), eq(alertType)))
            .thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any()))
            .thenReturn(true);

        // When
        locationTrackingService.markAlertSent(deliveryId, alertType);

        // Then
        verify(setOperations).add(eq("geofence:delivery:100:alerts"), eq(alertType));
        verify(redisTemplate).expire(eq("geofence:delivery:100:alerts"), eq(2L), any());
    }

    @Test
    void testHasAlertBeenSent_ReturnsTrue_WhenAlertSent() {
        // Given
        Long deliveryId = 100L;
        String alertType = "1KM_AWAY";

        when(setOperations.isMember(anyString(), eq(alertType)))
            .thenReturn(true);

        // When
        boolean result = locationTrackingService.hasAlertBeenSent(deliveryId, alertType);

        // Then
        assertTrue(result);
        verify(setOperations).isMember(eq("geofence:delivery:100:alerts"), eq(alertType));
    }

    @Test
    void testHasAlertBeenSent_ReturnsFalse_WhenAlertNotSent() {
        // Given
        Long deliveryId = 100L;
        String alertType = "1KM_AWAY";

        when(setOperations.isMember(anyString(), eq(alertType)))
            .thenReturn(false);

        // When
        boolean result = locationTrackingService.hasAlertBeenSent(deliveryId, alertType);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetActiveDeliveriesForRider_ReturnsDispatchedAndDelivered() {
        // Given
        Long riderId = 1L;

        Delivery delivery1 = new Delivery();
        delivery1.setId(100L);
        delivery1.setRiderId(riderId);
        delivery1.setStatus(DeliveryStatus.DISPATCHED);

        Delivery delivery2 = new Delivery();
        delivery2.setId(101L);
        delivery2.setRiderId(riderId);
        delivery2.setStatus(DeliveryStatus.DELIVERED);

        Delivery delivery3 = new Delivery();
        delivery3.setId(102L);
        delivery3.setRiderId(riderId);
        delivery3.setStatus(DeliveryStatus.PENDING);

        List<Delivery> allDeliveries = Arrays.asList(delivery1, delivery2, delivery3);

        when(deliveryRepository.findByRiderIdAndStatusNot(riderId, DeliveryStatus.COMPLETED))
            .thenReturn(allDeliveries);

        // When
        List<Delivery> result = locationTrackingService.getActiveDeliveriesForRider(riderId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> d.getStatus() == DeliveryStatus.DISPATCHED));
        assertTrue(result.stream().anyMatch(d -> d.getStatus() == DeliveryStatus.DELIVERED));
        assertFalse(result.stream().anyMatch(d -> d.getStatus() == DeliveryStatus.PENDING));
    }

    @Test
    void testGetActiveDeliveriesForRider_ReturnsEmpty_WhenNoActiveDeliveries() {
        // Given
        Long riderId = 1L;

        Delivery delivery = new Delivery();
        delivery.setId(100L);
        delivery.setRiderId(riderId);
        delivery.setStatus(DeliveryStatus.PENDING);

        when(deliveryRepository.findByRiderIdAndStatusNot(riderId, DeliveryStatus.COMPLETED))
            .thenReturn(Arrays.asList(delivery));

        // When
        List<Delivery> result = locationTrackingService.getActiveDeliveriesForRider(riderId);

        // Then
        assertEquals(0, result.size());
    }

    @Test
    void testFormatETA_ValidMinutes_ReturnsFormattedText() {
        // When & Then
        assertEquals("5 minutes away", locationTrackingService.formatETA(5.0));
        assertEquals("10 minutes away", locationTrackingService.formatETA(9.8));
        assertEquals("1 minute away", locationTrackingService.formatETA(1.0));
        assertEquals("2 minutes away", locationTrackingService.formatETA(1.5));
        assertEquals("Arriving now", locationTrackingService.formatETA(0.5));
    }

    @Test
    void testFormatETA_NullMinutes_ReturnsUnavailable() {
        // When
        String result = locationTrackingService.formatETA(null);

        // Then
        assertEquals("ETA unavailable", result);
    }

    @Test
    void testAddRiderLocation_HandlesException_Gracefully() {
        // Given
        Long riderId = 1L;
        double latitude = 40.7128;
        double longitude = -74.0060;

        when(geoOperations.add(anyString(), any(RedisGeoCommands.GeoLocation.class)))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() ->
            locationTrackingService.addRiderLocation(riderId, latitude, longitude)
        );
    }

    @Test
    void testCalculateDistance_HandlesException_ReturnsNull() {
        // Given
        Long riderId = 1L;
        Long deliveryId = 100L;

        when(geoOperations.distance(anyString(), anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        Double result = locationTrackingService.calculateDistance(riderId, deliveryId);

        // Then
        assertNull(result);
    }

    @Test
    void testCheckGeofenceAlert_ArrivingThreshold_ReturnsArrivingAlert() {
        // Given
        Long deliveryId = 100L;
        double currentDistanceKm = 0.05; // Within 100m (0.1km) threshold

        when(setOperations.isMember(anyString(), anyString()))
            .thenReturn(false);

        // When
        Optional<GeofenceAlert> alert = locationTrackingService.checkGeofenceAlert(
            deliveryId,
            currentDistanceKm
        );

        // Then
        assertTrue(alert.isPresent());
        assertEquals("ARRIVING", alert.get().alertType());
        assertTrue(alert.get().message().contains("arriving soon"));
    }
}
