package com.finedine.riderservice.controller;

import com.finedine.riderservice.dto.GenericMessageResponse;
import com.finedine.riderservice.dto.LocationUpdateDTO;
import com.finedine.riderservice.dto.RiderResponse;
import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.enums.DeliveryStatus;
import com.finedine.riderservice.entity.Rider;
import com.finedine.riderservice.security.SecurityUser;
import com.finedine.riderservice.service.RiderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/riders")
public class RiderController {

    private final RiderService riderService;

    @GetMapping("/my-rider")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    public Rider myProfile(@AuthenticationPrincipal SecurityUser securityUser) {
        return riderService.myProfile(securityUser);
    }

    @PostMapping("/online")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    public GenericMessageResponse goOnline(@AuthenticationPrincipal SecurityUser securityUser) {
        return riderService.goOnline(securityUser);
    }

    @PostMapping("/offline")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    public GenericMessageResponse goOffline(@AuthenticationPrincipal SecurityUser securityUser) {
        return riderService.goOffline(securityUser);
    }

    @GetMapping("/available-riders")
    @ResponseStatus(HttpStatus.OK)
    Page<RiderResponse> getAvailableRiders(@PageableDefault Pageable pageable) {
        return riderService.getAvailableRiders(pageable);
    }

    @GetMapping("/delivery-requests")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    public Page<Delivery> myDeliveryRequests(@AuthenticationPrincipal SecurityUser securityUser,
                                             @PageableDefault Pageable pageable) {
        return riderService.getMyDeliveryRequests(securityUser, pageable);
    }

    @GetMapping("/pending-deliveries")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    Page<Delivery> getAllPendingDeliveries(@PageableDefault Pageable pageable) {
        return riderService.getAllAvailableDeliveries(pageable);
    }

    @PostMapping("/accept-delivery/{deliveryId}")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    public GenericMessageResponse acceptDelivery(@PathVariable Long deliveryId, @AuthenticationPrincipal SecurityUser securityUser) {
        return riderService.acceptDelivery(deliveryId, securityUser);
    }

    @PostMapping("/decline-delivery/{deliveryId}")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    GenericMessageResponse declineDelivery(@PathVariable Long deliveryId, @AuthenticationPrincipal SecurityUser securityUser) {
        return riderService.declineDelivery(deliveryId, securityUser);
    }

    @PostMapping("/assign-delivery/{deliveryId}")
    @ResponseStatus(HttpStatus.OK)
    GenericMessageResponse assignDeliveryToRider(@PathVariable Long deliveryId) {
        return riderService.assignDeliveryToRider(deliveryId);
    }

    @PostMapping("/update-location")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    GenericMessageResponse updateRiderLocation(@AuthenticationPrincipal SecurityUser securityUser,
                                               @RequestParam double lat,
                                               @RequestParam double lon) {
        return riderService.updateRiderLocation(securityUser, lat, lon);
    }


    @PostMapping("/delivery/{deliveryId}/status")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.OK)
    public GenericMessageResponse updateDeliveryStatus(@PathVariable Long deliveryId,
                                                       @RequestParam DeliveryStatus status,
                                                       @AuthenticationPrincipal SecurityUser securityUser) {
        return riderService.updateDeliveryStatus(deliveryId, status, securityUser);
    }
}