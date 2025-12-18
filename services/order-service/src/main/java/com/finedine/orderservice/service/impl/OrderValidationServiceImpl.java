package com.finedine.orderservice.service.impl;

import com.finedine.orderservice.dto.CreateOrderRequest;
import com.finedine.orderservice.dto.OrderItemRequest;
import com.finedine.orderservice.exceptions.BadRequestException;
import com.finedine.orderservice.service.OrderValidationService;
import org.springframework.stereotype.Service;

import static com.finedine.orderservice.util.CustomMessages.*;

@Service
public class OrderValidationServiceImpl implements OrderValidationService {

    @Override
    public void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request.restaurantId() == null || request.restaurantId() <= 0) {
            throw new BadRequestException(INVALID_RESTAURANT_ID);
        }

        if (request.orderItems() == null || request.orderItems().isEmpty()) {
            throw new BadRequestException(INVALID_ORDER_ITEMS);
        }

        for (OrderItemRequest item : request.orderItems()) {
            validateOrderItem(item);
        }
    }

    private void validateOrderItem(OrderItemRequest item) {
        if (item.menuItemId() == null || item.menuItemId() <= 0) {
            throw new BadRequestException(INVALID_MENU_ITEM_ID);
        }

        if (item.quantity() == null || item.quantity() <= 0) {
            throw new BadRequestException(INVALID_QUANTITY);
        }
    }
}
