package com.finedine.orderservice.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OrderItem {

    @Id
    private Long id;
}
