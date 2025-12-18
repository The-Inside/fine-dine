package com.finedine.orderservice.service;

import com.finedine.orderservice.dto.CreateOrderRequest;

public interface OrderValidationService {
    void validateCreateOrderRequest(CreateOrderRequest request);
}
