package com.finedine.orderservice.service.impl;

import com.finedine.orderservice.repository.OrderRepository;
import com.finedine.orderservice.service.OrderValidationService;
import com.finedine.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTests {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderValidationService orderValidationService;

}
