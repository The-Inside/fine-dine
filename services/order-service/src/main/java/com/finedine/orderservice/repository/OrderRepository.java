package com.finedine.orderservice.repository;

import com.finedine.orderservice.entities.Order;
import org.springframework.data.repository.ListCrudRepository;

public interface OrderRepository extends ListCrudRepository<Order, Long> {
}
