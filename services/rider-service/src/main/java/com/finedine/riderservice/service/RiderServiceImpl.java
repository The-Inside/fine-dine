package com.finedine.riderservice.service;

import com.finedine.riderservice.dto.*;
import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.enums.Availability;
import com.finedine.riderservice.enums.DeliveryStatus;
import com.finedine.riderservice.entity.Rider;
import com.finedine.riderservice.enums.Status;
import com.finedine.riderservice.enums.VehicleType;
import com.finedine.riderservice.exception.NotFoundException;
import com.finedine.riderservice.exception.UnauthorizedException;
import com.finedine.riderservice.repository.DeliveryRepository;
import com.finedine.riderservice.repository.RiderRepository;
import com.finedine.riderservice.security.SecurityUser;
import com.finedine.riderservice.util.RiderMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.finedine.riderservice.enums.VehicleType.MOTORCYCLE;
import static com.finedine.riderservice.util.CustomMessages.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {
    private final DeliveryRepository deliveryRepository;
    private final RiderRepository riderRepository;
    private final RiderMapper riderMapper;
    private final RestTemplate restTemplate;
    private final LocationTrackingService locationTrackingService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * {@inheritDoc}
     */
    @SqsListener(value = "fds-rider-registration-queue.fifo")
    @Override
    public Rider createRider(RiderRegistrationQueue data) {

        Rider rider = riderMapper.toRider(data);
        rider.setStatus(Status.ONLINE);

        return riderRepository.save(rider);
    }


    /**
     * {@inheritDoc}
     */
    @SqsListener(value = "fds-delivery-request-queue.fifo")
    @Override
    public Delivery createDelivery(DeliveryRequestDTO request) {

        Delivery delivery = riderMapper.toDelivery(request);
        delivery.setStatus(DeliveryStatus.PENDING);

        return deliveryRepository.save(delivery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rider myProfile(SecurityUser securityUser) {

        Rider rider = findRiderIfExists(securityUser.externalId());

        validateRider(securityUser, rider);
        return rider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse goOnline(SecurityUser securityUser) {
        Rider rider = findRiderIfExists(securityUser.externalId());
        validateRider(securityUser, rider);

        rider.setStatus(Status.ONLINE);
        riderRepository.save(rider);

        return new GenericMessageResponse(RIDER_ONLINE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse goOffline(SecurityUser securityUser) {
        Rider rider = findRiderIfExists(securityUser.externalId());
        validateRider(securityUser, rider);

        rider.setStatus(Status.OFFLINE);
        riderRepository.save(rider);

        // Send tracking stopped notifications for all active deliveries
        List<Delivery> activeDeliveries = locationTrackingService.getActiveDeliveriesForRider(rider.getId());
        for (Delivery delivery : activeDeliveries) {
            try {
                Map<String, String> notification = Map.of(
                    "status", "TRACKING_STOPPED",
                    "reason", "RIDER_OFFLINE",
                    "message", TRACKING_STOPPED
                );

                String topic = "/topic/delivery/" + delivery.getId() + "/location";
                messagingTemplate.convertAndSend(topic, notification);

                log.info("Sent tracking stopped notification for delivery {} (rider went offline)",
                    delivery.getId());
            } catch (Exception e) {
                log.error("Failed to send tracking stopped notification for delivery {}: {}",
                    delivery.getId(), e.getMessage());
            }
        }

        return new GenericMessageResponse(RIDER_OFFLINE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<RiderResponse> getAvailableRiders(Pageable pageable) {
        var results = riderRepository.findOnlineAndAvailableRiders(pageable);
        return results.map(riderMapper::toRiderResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Delivery> getMyDeliveryRequests(SecurityUser securityUser, Pageable pageable) {
        Rider rider = findRiderIfExists(securityUser.externalId());

        checkOnline(rider.getStatus());
        checkAvailable(rider.getAvailability());

        return deliveryRepository.findByRiderIdAndStatus(rider.getId(), pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Delivery> getAllAvailableDeliveries(Pageable pageable) {
        return deliveryRepository.findByStatus(DeliveryStatus.PENDING, pageable);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse assignDeliveryToRider(Long deliveryId) {

        Delivery deliveryRequest = findDeliveryIfExists(deliveryId);

        List<Rider> availableRiders = riderRepository.findByAvailable();

        //todo: change to redis or cache for better performance
        Rider bestRider = selectBestRider(availableRiders, deliveryRequest.getRestaurantLat(), deliveryRequest.getRestaurantLon());

        if (bestRider == null) throw new NotFoundException(NO_AVAILABLE_RIDERS);

        deliveryRequest.setRiderId(bestRider.getId());
        deliveryRequest.setStatus(DeliveryStatus.REQUESTED);

        deliveryRepository.save(deliveryRequest);

        log.info("Requested order request {} to rider {}", deliveryRequest.getOrderId(), bestRider.getId());

        return new GenericMessageResponse("Delivery assigned to rider " + bestRider.getId() + " and awaiting acceptance");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse acceptDelivery(Long deliveryId,  SecurityUser securityUser) {
        Delivery delivery = findDeliveryIfExists(deliveryId);

        Rider rider = riderRepository.findById(delivery.getRiderId())
                .orElseThrow(() -> new NotFoundException(RIDER_NOT_FOUND));

        validateRider(securityUser, rider);

        delivery.setStatus(DeliveryStatus.ACCEPTED);

        checkOnline(rider.getStatus());
        checkAvailable(rider.getAvailability());

        rider.setAvailability(Availability.BUSY);

        deliveryRepository.save(delivery);
        riderRepository.save(rider);

        return new GenericMessageResponse(DELIVERY_ACCEPTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse declineDelivery(Long deliveryId, SecurityUser securityUser){
        Delivery delivery = findDeliveryIfExists(deliveryId);

        Rider rider = riderRepository.findById(delivery.getRiderId())
                .orElseThrow(() -> new NotFoundException(RIDER_NOT_FOUND));

        validateRider(securityUser, rider);

        delivery.setRiderId(null);
        delivery.setStatus(DeliveryStatus.PENDING);

        deliveryRepository.save(delivery);

        return new GenericMessageResponse(REQUEST_DECLINED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse updateRiderLocation(SecurityUser securityUser, double lat, double lon) {
        Rider rider = findRiderIfExists(securityUser.externalId());

        rider.setLatitude(lat);
        rider.setLongitude(lon);

        riderRepository.save(rider);

        // Update location in Redis
        locationTrackingService.addRiderLocation(rider.getId(), lat, lon);

        // Broadcast to customers tracking this rider
        broadcastLocationToCustomers(rider);

        log.info("Rider {} location updated: {}, {}", rider.getId(), lat, lon);

        return new GenericMessageResponse(LOCATION_UPDATED);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse updateDeliveryStatus(Long deliveryId, DeliveryStatus status, SecurityUser securityUser) {
        Delivery delivery = findDeliveryIfExists(deliveryId);
        Rider rider = findRiderIfExists(securityUser.externalId());

        checkAssignedRider(rider.getId(), delivery.getRiderId());

        if (rider.getAvailability() != Availability.BUSY) {
            throw new UnauthorizedException(RIDER_MUST_BE_BUSY);
        }

        DeliveryStatus oldStatus = delivery.getStatus();
        delivery.setStatus(status);
        deliveryRepository.save(delivery);

        // Handle tracking stop scenarios
        if (status == DeliveryStatus.DELIVERED || status == DeliveryStatus.COMPLETED
                || status == DeliveryStatus.CANCELLED) {

            rider.setAvailability(Availability.AVAILABLE);
            riderRepository.save(rider);

            // Send tracking stop notification to customer
            sendTrackingStoppedNotification(delivery, status);
        }

        return new GenericMessageResponse("Delivery status updated to " + status);
    }

    /**
     * Send tracking stopped notification to customer
     */
    private void sendTrackingStoppedNotification(Delivery delivery, DeliveryStatus reason) {
        try {
            String reasonText;
            if (reason == DeliveryStatus.DELIVERED) {
                reasonText = "DELIVERY_COMPLETED";
            } else if (reason == DeliveryStatus.CANCELLED) {
                reasonText = "DELIVERY_CANCELLED";
            } else {
                reasonText = "DELIVERY_COMPLETED";
            }

            Map<String, String> notification = Map.of(
                "status", "TRACKING_STOPPED",
                "reason", reasonText,
                "message", TRACKING_STOPPED
            );

            String topic = "/topic/delivery/" + delivery.getId() + "/location";
            messagingTemplate.convertAndSend(topic, notification);

            log.info("Sent tracking stopped notification for delivery {} (reason: {})",
                delivery.getId(), reasonText);
        } catch (Exception e) {
            log.error("Failed to send tracking stopped notification: {}", e.getMessage());
        }
    }


    private Rider findRiderIfExists(String externalId) {
        return riderRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NotFoundException(RIDER_NOT_FOUND));
    }

    private Delivery findDeliveryIfExists(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException(DELIVERY_NOT_FOUND));
    }

    private void validateRider(SecurityUser securityUser, Rider rider) {
        if (securityUser.externalId() != null && !securityUser.externalId().equals(rider.getExternalId())) {
            throw new UnauthorizedException(RESTRICTED_ACTION);
        }
    }

    private void checkAssignedRider(Long deliveryRiderId, Long riderId) {
        if( deliveryRiderId == null || !deliveryRiderId.equals(riderId)) {
            throw new UnauthorizedException(RESTRICTED_ACTION);
        }
    }

    private void checkOnline(Status status) {
        if( status != Status.ONLINE) {
            throw new UnauthorizedException(RIDER_MUST_BE_ONLINE);
        }
    }

    private void checkAvailable(Availability availability) {
        if( availability != Availability.AVAILABLE) {
            throw new UnauthorizedException(RIDER_MUST_BE_AVAILABLE);
        }
    }

    private boolean isRiderAvailable(Rider rider) {
        return rider.getAvailability() == Availability.AVAILABLE;
    }

    private Rider selectBestRider(List<Rider> riders, double restLat, double restLon) {
        return riders.stream()
                .map(r -> {
                    double etaMinutes = calculateETA(r.getLatitude(), r.getLongitude(), restLat, restLon);
                    double score = calculateScore(r.getRating(), etaMinutes);
                    return new AbstractMap.SimpleEntry<>(r, score);
                })
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private double calculateScore(double rating, double etaMinutes) {
        double etaPenalty = etaMinutes / 10.0;
        return rating - etaPenalty;
    }

    private double calculateETA(double riderLat, double riderLon, double restLat, double restLon) {
        double distanceKm = haversine(riderLat, riderLon, restLat, restLon);
        double avgSpeedKmH = 30.0;
        return (distanceKm / avgSpeedKmH) * 60.0;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRiderLocationWebSocket(SecurityUser securityUser, RiderLocationUpdateRequest request) {
        Rider rider = findRiderIfExists(securityUser.externalId());

        Instant now = Instant.now();
        Instant updateTime = request.timestamp();

        if (Duration.between(updateTime, now).toMinutes() > 5) {
            log.warn("Location request too stale for rider {}: {} minutes old",
                    rider.getId(), Duration.between(updateTime, now).toMinutes());
            throw new IllegalArgumentException(LOCATION_UPDATE_TOO_STALE);
        }

//        rider.setLatitude(request.latitude());
//        rider.setLongitude(request.longitude());
//        riderRepository.save(rider);

        locationTrackingService.addRiderLocation(rider.getId(), request.latitude(), request.longitude());

        broadcastLocationToCustomers(rider);

        log.info("Rider {} location updated via WebSocket: ({}, {})", rider.getId(), request.latitude(), request.longitude());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastLocationToCustomers(Rider rider) {
        List<Delivery> activeDeliveries = locationTrackingService.getActiveDeliveriesForRider(rider.getId());

        if (activeDeliveries.isEmpty()) {
            log.debug("No active deliveries for rider {}, skipping broadcast", rider.getId());
            return;
        }

        for (Delivery delivery : activeDeliveries) {
            try {
                // Ensure customer location is in Redis
                locationTrackingService.addCustomerLocation(
                    delivery.getId(),
                    delivery.getCustomerLat(),
                    delivery.getCustomerLon()
                );

                // Calculate ETA
                Double etaMinutes = locationTrackingService.calculateETA(rider.getId(), delivery.getId());
                String etaText = locationTrackingService.formatETA(etaMinutes);

                // Create location response
                RiderLocationResponse response = new RiderLocationResponse(
                    rider.getId(),
                    rider.getLatitude(),
                    rider.getLongitude(),
                    System.currentTimeMillis(),
                    etaMinutes,
                    etaText
                );

                // Broadcast to delivery-specific topic
                String topic = "/topic/delivery/" + delivery.getId() + "/location";
                messagingTemplate.convertAndSend(topic, response);

                log.debug("Broadcasted location for rider {} to delivery {} (ETA: {})",
                    rider.getId(), delivery.getId(), etaText);

                // Check geofence alerts
                if (etaMinutes != null) {
                    Double distanceKm = locationTrackingService.calculateDistance(rider.getId(), delivery.getId());

                    if (distanceKm != null) {
                        var alert = locationTrackingService.checkGeofenceAlert(
                            delivery.getId(),
                            distanceKm
                        );

                        if (alert.isPresent()) {
                            GeofenceAlert geofenceAlert = alert.get();
                            String alertTopic = "/topic/delivery/" + delivery.getId() + "/alerts";
                            messagingTemplate.convertAndSend(alertTopic, geofenceAlert);

                            // Mark alert as sent
                            locationTrackingService.markAlertSent(
                                delivery.getId(),
                                geofenceAlert.alertType()
                            );

                            log.info("Sent geofence alert {} for delivery {}",
                                geofenceAlert.alertType(), delivery.getId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to broadcast location for delivery {}: {}",
                    delivery.getId(), e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TrackingValidationResponse validateOrderForTracking(Long orderId, SecurityUser securityUser) {
        log.info("Customer validating order {} for tracking", orderId);

        Delivery delivery = findDeliveryByOrderIdIfExists(orderId);

        if (!delivery.getCustomerId().equals(securityUser.externalId())) {
            throw new UnauthorizedException(ORDER_DOES_NOT_BELONG_TO_YOU);
        }

        if (!isDeliveryTrackable(delivery)) {
            throw new IllegalArgumentException(
                DELIVERY_NOT_TRACKABLE + ". Current status: " + delivery.getStatus()
            );
        }

        Rider rider = riderRepository.findById(delivery.getRiderId())
            .orElseThrow(() -> new NotFoundException(RIDER_NOT_FOUND));

        if (rider.getStatus() != Status.ONLINE) {
            throw new IllegalStateException(RIDER_IS_OFFLINE);
        }

        String riderName = rider.getFirstName() + " " + rider.getLastName();
        String subscriptionTopic = "/topic/delivery/" + delivery.getId() + "/location";

        log.info("Order {} validated for tracking. Delivery ID: {}, Rider ID: {}", orderId, delivery.getId(), rider.getId());

        return new TrackingValidationResponse(
            rider.getId(),
            delivery.getId(),
            riderName,
            rider.getVehicleType() != null ? rider.getVehicleType().toString() : VehicleType.MOTORCYCLE.toString(),
            subscriptionTopic
        );
    }

    private boolean isDeliveryTrackable(Delivery delivery) {
        return delivery.getStatus() == DeliveryStatus.DISPATCHED || delivery.getStatus() == DeliveryStatus.DELIVERED;
    }

    private Delivery findDeliveryByOrderIdIfExists(Long orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(DELIVERY_NOT_FOUND));
    }
}
