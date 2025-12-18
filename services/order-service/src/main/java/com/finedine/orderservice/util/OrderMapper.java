package com.finedine.orderservice.util;

import com.finedine.orderservice.dto.CreateOrderRequest;
import com.finedine.orderservice.dto.OrderItemResponse;
import com.finedine.orderservice.dto.OrderResponse;
import com.finedine.orderservice.entities.Order;
import com.finedine.orderservice.entities.OrderItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
