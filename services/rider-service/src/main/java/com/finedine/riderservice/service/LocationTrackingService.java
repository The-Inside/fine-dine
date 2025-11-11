package com.finedine.riderservice.service;

import com.finedine.riderservice.dto.GeofenceAlert;
import com.finedine.riderservice.entity.Delivery;

import java.util.List;
import java.util.Optional;

public interface LocationTrackingService {
    /**
     * Add or update rider's location in Redis
     * @param riderId The rider's ID
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     */
    void addRiderLocation(Long riderId, double latitude, double longitude);

    /**
     * Add customer/delivery destination location in Redis
     * @param deliveryId The delivery ID
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     */
    void addCustomerLocation(Long deliveryId, double latitude, double longitude);

    /**
     * Calculate distance between rider and customer using Redis GEODIST
     * @param riderId The rider's ID
     * @param deliveryId The delivery ID
     * @return Distance in kilometers, or null if calculation fails
     */
    Double calculateDistance(Long riderId, Long deliveryId);

    /**
     * Calculate ETA based on distance and average speed
     * @param riderId The rider's ID
     * @param deliveryId The delivery ID
     * @return ETA in minutes, or null if calculation fails
     */
    Double calculateETA(Long riderId, Long deliveryId);

    /**
     * Check if any geofence threshold has been crossed
     * @param deliveryId The delivery ID
     * @param currentDistanceKm The current distance in kilometers
     * @return GeofenceAlert if threshold crossed, empty otherwise
     */
    Optional<GeofenceAlert> checkGeofenceAlert(Long deliveryId, double currentDistanceKm);

    /**
     * Mark a geofence alert as sent to prevent duplicates
     * @param deliveryId The delivery ID
     * @param alertType The alert type (e.g., "1KM_AWAY")
     */
    void markAlertSent(Long deliveryId, String alertType);

    /**
     * Check if a geofence alert has already been sent
     * @param deliveryId The delivery ID
     * @param alertType The alert type to check
     * @return true if alert was already sent, false otherwise
     */
    boolean hasAlertBeenSent(Long deliveryId, String alertType);

    /**
     * Get all active deliveries for a rider (DISPATCHED or DELIVERED status)
     * @param riderId The rider's ID
     * @return List of active deliveries
     */
    List<Delivery> getActiveDeliveriesForRider(Long riderId);

    /**
     * Format ETA minutes into readable text
     * @param etaMinutes The ETA in minutes
     * @return Formatted text like "5 minutes away"
     */
    String formatETA(Double etaMinutes);
}
