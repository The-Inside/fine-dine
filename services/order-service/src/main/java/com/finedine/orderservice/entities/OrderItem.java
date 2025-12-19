package com.finedine.orderservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

}
