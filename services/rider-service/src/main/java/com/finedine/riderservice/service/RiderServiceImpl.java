package com.finedine.riderservice.service;

import com.finedine.riderservice.dto.*;
import com.finedine.riderservice.entity.Availability;
import com.finedine.riderservice.entity.DeliveryStatus;
import com.finedine.riderservice.entity.Rider;
import com.finedine.riderservice.entity.Status;
import com.finedine.riderservice.exception.NotFoundException;
import com.finedine.riderservice.exception.UnauthorizedException;
import com.finedine.riderservice.repository.RiderRepository;
import com.finedine.riderservice.security.SecurityUser;
import com.finedine.riderservice.util.RiderMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.finedine.riderservice.util.CustomMessages.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {

    private final RiderRepository riderRepository;
    private final RiderMapper riderMapper;
    private final RestTemplate restTemplate;

    /**
     * {@inheritDoc}
     */
    @SqsListener(value = "fds-rider-registration-queue.fifo")
    @Override
    public void createRider(RiderRegistrationQueue data) {

        Rider rider = riderMapper.toRider(data);
        rider.setStatus(Status.ONLINE);

        riderRepository.save(rider);
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

        return new GenericMessageResponse(RIDER_OFFLINE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OrderRequest> getAvailableDeliveryRequests(SecurityUser securityUser) {
        Rider rider = findRiderIfExists(securityUser.externalId());
        if (rider.getStatus() != Status.ONLINE) {
            throw new UnauthorizedException(RIDER_MUST_BE_ONLINE);
        }

        // Call Order Service to fetch available order requests for rider
        return restTemplate.exchange(
                "http://order-service/api/v1/orders/available",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<OrderRequest>>() {}
        ).getBody();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse acceptDelivery(Long orderId, SecurityUser securityUser) {
        Rider rider = findRiderIfExists(securityUser.externalId());
        if (rider.getStatus() != Status.ONLINE || rider.getAvailability() != Availability.AVAILABLE) {
            throw new UnauthorizedException(RIDER_MUST_BE_ONLINE_AND_AVAILABLE);
        }
        rider.setAvailability(Availability.BUSY);
        riderRepository.save(rider);

        // Call Order Service to update order with assigned_rider_id and status
        restTemplate.postForObject(
                "http://order-service/api/v1/orders/" + orderId + "/assign",
                new OrderAssignmentDTO(orderId, rider.getId(), "DISPATCHED"),
                Void.class
        );
        return new GenericMessageResponse(DELIVERY_REQUEST_ACCEPTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String updateLocation(SecurityUser securityUser, String location) {
        Rider rider = findRiderIfExists(securityUser.externalId());
        rider.setCurrentLocation(location);
        riderRepository.save(rider);
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericMessageResponse updateDeliveryStatus(Long orderId, DeliveryStatus status, SecurityUser securityUser) {
        Rider rider = findRiderIfExists(securityUser.externalId());
        if (rider.getAvailability() != Availability.BUSY) {
            throw new UnauthorizedException(RIDER_MUST_BE_BUSY);
        }

        // Call Order Service to update order status
        restTemplate.postForObject(
                "http://order-service/api/v1/orders/" + orderId + "/status",
                new OrderStatusDTO(orderId, status),
                Void.class
        );

        if (status == DeliveryStatus.DELIVERED || status == DeliveryStatus.CANCELLED) {
            rider.setAvailability(Availability.AVAILABLE);
            riderRepository.save(rider);
        }
        return new GenericMessageResponse(DELIVERY_REQUEST_UPDATED + status.toString());
    }

    private Rider findRiderIfExists(String externalId) {
        return riderRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NotFoundException(RIDER_NOT_FOUND));
    }

    private void validateRider(SecurityUser securityUser, Rider rider) {
        if (securityUser.externalId() != null && !securityUser.externalId().equals(rider.getExternalId())) {
            throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
        }
    }
}
