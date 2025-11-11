package com.finedine.riderservice.service;

import com.finedine.riderservice.dto.GeofenceAlert;
import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.enums.DeliveryStatus;
import com.finedine.riderservice.enums.GeofenceThreshold;
import com.finedine.riderservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.domain.geo.Distance;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationTrackingServiceImpl implements LocationTrackingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeliveryRepository deliveryRepository;

    private static final String LOCATIONS_KEY = "locations:all"; // Single key for all locations
    private static final String GEOFENCE_ALERT_KEY_PREFIX = "geofence:delivery:";
    private static final double AVERAGE_SPEED_KMH = 30.0; // Average speed in km/h
    private static final long LOCATION_TTL_HOURS = 1; // TTL for location data

    // Prefixes for member names in Redis
    private static final String RIDER_PREFIX = "rider:";
    private static final String DELIVERY_PREFIX = "delivery:";

    @Override
    public void addRiderLocation(Long riderId, double latitude, double longitude) {
        try {
            redisTemplate.opsForGeo().add(
                LOCATIONS_KEY,
                new RedisGeoCommands.GeoLocation<>(
                    RIDER_PREFIX + riderId,  // e.g., "rider:1"
                    new Point(longitude, latitude)
                )
            );
            // Set TTL on the key
            redisTemplate.expire(LOCATIONS_KEY, LOCATION_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Added rider {} location to Redis: ({}, {})", riderId, latitude, longitude);
        } catch (Exception e) {
            log.error("Failed to add rider location to Redis: {}", e.getMessage());
        }
    }

    @Override
    public void addCustomerLocation(Long deliveryId, double latitude, double longitude) {
        try {
            redisTemplate.opsForGeo().add(
                LOCATIONS_KEY,
                new RedisGeoCommands.GeoLocation<>(
                    DELIVERY_PREFIX + deliveryId,
                    new Point(longitude, latitude)
                )
            );
            // Set TTL on the key
            redisTemplate.expire(LOCATIONS_KEY, LOCATION_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Added customer location for delivery {} to Redis: ({}, {})", deliveryId, latitude, longitude);
        } catch (Exception e) {
            log.error("Failed to add customer location to Redis: {}", e.getMessage());
        }
    }

    @Override
    public Double calculateDistance(Long riderId, Long deliveryId) {
        try {
            // Use Redis GEODIST with prefixed member names - NO Haversine needed!
            Distance distance = redisTemplate.opsForGeo().distance(
                LOCATIONS_KEY,                // Single key for all locations
                RIDER_PREFIX + riderId,       // e.g., "rider:1"
                DELIVERY_PREFIX + deliveryId, // e.g., "delivery:100"
                Metrics.KILOMETERS
            );

            if (distance != null) {
                log.debug("Distance between rider {} and delivery {}: {} km", riderId, deliveryId, distance.getValue());
                return distance.getValue();
            }

            log.warn("Could not calculate distance - locations not found in Redis for rider {} or delivery {}", riderId, deliveryId);
            return null;
        } catch (Exception e) {
            log.error("Failed to calculate distance using Redis GEODIST: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Double calculateETA(Long riderId, Long deliveryId) {
        Double distance = calculateDistance(riderId, deliveryId);
        if (distance == null) {
            log.warn("Cannot calculate ETA: distance is null for rider {} and delivery {}",
                riderId, deliveryId);
            return null;
        }

        // ETA in minutes = (distance in km / speed in km/h) * 60
        double etaMinutes = (distance / AVERAGE_SPEED_KMH) * 60;
        log.debug("Calculated ETA for rider {} to delivery {}: {} minutes",
            riderId, deliveryId, etaMinutes);
        return etaMinutes;
    }

    @Override
    public Optional<GeofenceAlert> checkGeofenceAlert(Long deliveryId, double currentDistanceKm) {
        // Check thresholds from largest to smallest
        for (GeofenceThreshold threshold : GeofenceThreshold.values()) {
            if (currentDistanceKm <= threshold.getDistanceKm()) {
                // Check if this alert has already been sent
                if (!hasAlertBeenSent(deliveryId, threshold.getAlertType())) {
                    GeofenceAlert alert = new GeofenceAlert(
                        threshold.getAlertType(),
                        currentDistanceKm,
                        threshold.getMessage()
                    );
                    log.info("Geofence alert triggered for delivery {}: {}",
                        deliveryId, threshold.getAlertType());
                    return Optional.of(alert);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void markAlertSent(Long deliveryId, String alertType) {
        try {
            String key = GEOFENCE_ALERT_KEY_PREFIX + deliveryId + ":alerts";
            redisTemplate.opsForSet().add(key, alertType);
            // Set TTL of 2 hours for alert tracking
            redisTemplate.expire(key, 2, TimeUnit.HOURS);
            log.debug("Marked alert {} as sent for delivery {}", alertType, deliveryId);
        } catch (Exception e) {
            log.error("Failed to mark alert as sent: {}", e.getMessage());
        }
    }

    @Override
    public boolean hasAlertBeenSent(Long deliveryId, String alertType) {
        try {
            String key = GEOFENCE_ALERT_KEY_PREFIX + deliveryId + ":alerts";
            Boolean isMember = redisTemplate.opsForSet().isMember(key, alertType);
            return isMember != null && isMember;
        } catch (Exception e) {
            log.error("Failed to check if alert was sent: {}", e.getMessage());
            return false; // Assume not sent if check fails
        }
    }

    @Override
    public List<Delivery> getActiveDeliveriesForRider(Long riderId) {
        List<DeliveryStatus> activeStatuses = Arrays.asList(
                DeliveryStatus.DISPATCHED,
                DeliveryStatus.DELIVERED
        );

        return deliveryRepository.findByRiderIdAndStatusIn(riderId, activeStatuses);
    }

    @Override
    public String formatETA(Double etaMinutes) {
        if (etaMinutes == null) {
            return "ETA unavailable";
        }

        long roundedMinutes = Math.round(Math.ceil(etaMinutes));

        if (roundedMinutes < 1) {
            return "Arriving now";
        } else if (roundedMinutes == 1) {
            return "1 minute away";
        } else {
            return roundedMinutes + " minutes away";
        }
    }

}
