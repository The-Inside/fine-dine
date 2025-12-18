package com.finedine.orderservice.service;

import com.finedine.orderservice.dto.CreateOrderRequest;
import com.finedine.orderservice.dto.OrderResponse;
import com.finedine.orderservice.security.SecurityUser;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, SecurityUser securityUser);
}
