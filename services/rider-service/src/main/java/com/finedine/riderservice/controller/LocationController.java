package com.finedine.riderservice.controller;


import com.finedine.riderservice.security.SecurityUser;
import com.finedine.riderservice.service.RiderService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LocationController {
    private final RiderService riderService;

//    @MessageMapping("/location")
//    @SendTo("/topic/rider-location")
//    public String updateLocation(@AuthenticationPrincipal SecurityUser securityUser, String location) {
//        return riderService.updateLocation(securityUser, location);
//    }
}
