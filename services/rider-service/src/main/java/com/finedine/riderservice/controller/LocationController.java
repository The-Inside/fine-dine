package com.finedine.riderservice.controller;

import com.finedine.riderservice.dto.RiderLocationUpdateRequest;
import com.finedine.riderservice.security.SecurityUser;
import com.finedine.riderservice.service.RiderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LocationController {
    private final RiderService riderService;

    /**
     * Handle rider location updates via WebSocket
     * Riders send their location to /app/rider/location
     */
    @MessageMapping("/rider/location")
    public void updateRiderLocation(
        @AuthenticationPrincipal SecurityUser securityUser,
        @Payload @Valid RiderLocationUpdateRequest request
    ) {
        log.debug("Received location update from rider via WebSocket");
        riderService.updateRiderLocationWebSocket(securityUser, request);
    }
}
