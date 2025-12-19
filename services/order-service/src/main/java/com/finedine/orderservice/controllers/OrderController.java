package com.finedine.orderservice.controllers;

import com.finedine.orderservice.dto.CreateOrderRequest;
import com.finedine.orderservice.dto.OrderResponse;
import com.finedine.orderservice.security.SecurityUser;
import com.finedine.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request,
                                                     @AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Received create order request from customer: {}", securityUser.externalId());
        return orderService.createOrder(request, securityUser);
    }
}
