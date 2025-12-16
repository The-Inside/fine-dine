package com.finedine.orderservice.repository;

import com.finedine.orderservice.entities.OrderItem;
import org.springframework.data.repository.ListCrudRepository;

public interface OrderItemRepository extends ListCrudRepository<OrderItem, Long> {
}
