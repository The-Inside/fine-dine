package com.finedine.orderservice.service.impl;

import com.finedine.orderservice.config.RestaurantServiceClient;
import com.finedine.orderservice.dto.CreateOrderRequest;
import com.finedine.orderservice.dto.MenuItemDTO;
import com.finedine.orderservice.dto.OrderItemRequest;
import com.finedine.orderservice.dto.OrderResponse;
import com.finedine.orderservice.entities.Order;
import com.finedine.orderservice.entities.OrderItem;
import com.finedine.orderservice.enums.OrderStatus;
import com.finedine.orderservice.exceptions.BadRequestException;
import com.finedine.orderservice.repository.OrderRepository;
import com.finedine.orderservice.security.SecurityUser;
import com.finedine.orderservice.service.OrderService;
import com.finedine.orderservice.service.OrderValidationService;
import com.finedine.orderservice.util.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderValidationService orderValidationService;
    private final OrderMapper orderMapper;
    private final RestaurantServiceClient restaurantServiceClient;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, SecurityUser securityUser) {
        log.info("Creating order for customer: {}, restaurant: {}",
                securityUser.externalId(), request.restaurantId());

        orderValidationService.validateCreateOrderRequest(request);

        // Fetch menu items with prices from restaurant service using HTTP Interface
        List<Long> menuItemIds = request.orderItems().stream()
                .map(OrderItemRequest::menuItemId)
                .toList();

        List<MenuItemDTO> menuItems = restaurantServiceClient.getMenuItemsByIds(menuItemIds);
        log.info("Fetched {} menu items from restaurant service", menuItems.size());
        log.info("Menu items: {}", menuItems);

        // Validate all menu items exist and are available
        validateMenuItems(menuItems, request);

        // Create a map for quick price lookup
        Map<Long, MenuItemDTO> menuItemMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItemDTO::id, item -> item));

        Order order = Order.builder()
                .customerExternalId(securityUser.externalId())
                .restaurantId(request.restaurantId())
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = createOrderItems(request.orderItems(), order, menuItemMap);
        order.setOrderItems(orderItems);

        BigDecimal totalAmount = calculateTotalAmount(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully with ID: {} for customer: {}, total: {}",
                savedOrder.getId(), securityUser.externalId(), totalAmount);

        return orderMapper.toOrderResponse(savedOrder);
    }

    private void validateMenuItems(List<MenuItemDTO> menuItems, CreateOrderRequest request) {
        if (menuItems == null || menuItems.size() != request.orderItems().size()) {
            log.error("Some menu items not found. Expected: {}, Found: {}",
                    request.orderItems().size(), menuItems != null ? menuItems.size() : 0);
            throw new BadRequestException("One or more menu items not found");
        }

        // Validate all items belong to the same restaurant
        boolean allFromSameRestaurant = menuItems.stream()
                .allMatch(item -> item.restaurantId().equals(request.restaurantId()));

        if (!allFromSameRestaurant) {
            log.error("Menu items do not belong to restaurant: {}", request.restaurantId());
            throw new BadRequestException("All menu items must belong to the specified restaurant");
        }

        // Validate all items are available
        List<Long> unavailableItems = menuItems.stream()
                .filter(item -> !item.isAvailable())
                .map(MenuItemDTO::id)
                .toList();

        if (!unavailableItems.isEmpty()) {
            log.error("Some menu items are not available: {}", unavailableItems);
            throw new BadRequestException("Some menu items are not available: " + unavailableItems);
        }
    }

    private List<OrderItem> createOrderItems(List<OrderItemRequest> itemRequests,
                                             Order order,
                                             Map<Long, MenuItemDTO> menuItemMap) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            MenuItemDTO menuItem = menuItemMap.get(itemRequest.menuItemId());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .menuItemId(itemRequest.menuItemId())
                    .quantity(itemRequest.quantity())
                    .price(menuItem.price())
                    .build();
            items.add(item);
        }
        return items;
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}