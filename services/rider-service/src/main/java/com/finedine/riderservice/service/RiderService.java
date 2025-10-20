package com.finedine.riderservice.service;


import com.finedine.riderservice.dto.*;
import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.enums.DeliveryStatus;
import com.finedine.riderservice.entity.Rider;
import com.finedine.riderservice.security.SecurityUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RiderService {
    /**
     * Create a new rider registration request.
     *
     * @param data the rider registration data
     */
    Rider createRider(RiderRegistrationQueue data);

    /**
     * Create a new delivery assignment.
     *
     * @param request the delivery assignment data
     */
    Delivery createDelivery(DeliveryRequestDTO request);

    /**
     * Get the rider profile of the currently authenticated rider.
     *
     * @param securityUser the security user containing authentication details
     * @return the rider profile
     */
    Rider myProfile(SecurityUser securityUser);

    /**
     * Set the rider's status to online.
     *
     * @param securityUser the security user containing authentication details
     * @return a generic message response indicating success
     */
    GenericMessageResponse goOnline(SecurityUser securityUser);

    /**
     * Set the rider's status to offline.
     *
     * @param securityUser the security user containing authentication details
     * @return a generic message response indicating success
     */
    GenericMessageResponse goOffline(SecurityUser securityUser);

    /**
     * Get a paginated list of all available riders.
     *
     * @return a paginated list of rider responses
     */
    Page<RiderResponse> getAvailableRiders(Pageable pageable);

    /**
     * Get a list of available delivery requests for the rider.
     *
     * @param securityUser the security user containing authentication details
     * @return a list of available delivery requests
     */
    Page<Delivery> getMyDeliveryRequests(SecurityUser securityUser, Pageable pageable);

    /**
     * Get a list of all pending deliveries.
     *
     * @return a list of all delivery requests
     */
    Page<Delivery> getAllAvailableDeliveries(Pageable pageable);

    /**
     * Accept a delivery request.
     *
     * @param deliveryId the ID of the order to accept
     * @param securityUser the security user containing authentication details
     * @return a generic message response indicating success
     */
    GenericMessageResponse acceptDelivery(Long deliveryId, SecurityUser securityUser);

    /**
     * Decline a delivery request.
     *
     * @param deliveryId the ID of the order to decline
     * @param securityUser the security user containing authentication details
     * @return a generic message response indicating success
     */
    GenericMessageResponse declineDelivery(Long deliveryId, SecurityUser securityUser);

    /**
     * Assign a delivery to a rider.
     *
     * @param deliveryId the ID of the delivery to assign
     * @return a generic message response indicating success
     */
    GenericMessageResponse assignDeliveryToRider(Long deliveryId);

    /**
     * Update the rider's current location.
     *
     * @param securityUser the security user containing authentication details
     * @return a generic message response indicating success
     */
    GenericMessageResponse updateRiderLocation(SecurityUser securityUser, double lat, double  lon);


    /**
     * Update the status of a delivery.
     *
     * @param orderId the ID of the order to update
     * @param status the new status of the delivery
     * @param securityUser the security user containing authentication details
     * @return a generic message response indicating success
     */
    GenericMessageResponse updateDeliveryStatus(Long orderId, DeliveryStatus status, SecurityUser securityUser);
}
