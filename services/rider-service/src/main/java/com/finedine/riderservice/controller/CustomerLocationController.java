package com.finedine.riderservice.controller;

import com.finedine.riderservice.dto.TrackingValidationResponse;
import com.finedine.riderservice.security.SecurityUser;
import com.finedine.riderservice.service.RiderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerLocationController {
    private final RiderService riderService;

    /**
     * Validate order for tracking and return rider information
     * Customer calls this endpoint with their orderId to get tracking details
     *
     * @param orderId The order ID to track
     * @param securityUser The authenticated user (customer)
     * @return Tracking validation response with rider info and subscription topic
     */
    @PostMapping("/track/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<TrackingValidationResponse> validateOrderForTracking(
            @PathVariable Long orderId,
            @AuthenticationPrincipal SecurityUser securityUser) {

        TrackingValidationResponse response = riderService.validateOrderForTracking(orderId, securityUser);

        return ResponseEntity.ok(response);
    }
}
