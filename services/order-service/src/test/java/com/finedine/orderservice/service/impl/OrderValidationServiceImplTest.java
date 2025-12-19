package com.finedine.orderservice.service.impl;

import com.finedine.orderservice.dto.CreateOrderRequest;
import com.finedine.orderservice.dto.OrderItemRequest;
import com.finedine.orderservice.exceptions.BadRequestException;
import com.finedine.orderservice.service.OrderValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderValidationService Tests")
class OrderValidationServiceImplTest {

    private OrderValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new OrderValidationServiceImpl();
    }

    @Test
    @DisplayName("Should successfully validate a valid create order request")
    void testValidateCreateOrderRequest_Success() {
        // Arrange
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> validationService.validateCreateOrderRequest(request));
    }

    @Test
    @DisplayName("Should throw exception when restaurant ID is null")
    void testValidateCreateOrderRequest_NullRestaurantId() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 2));
        CreateOrderRequest request = new CreateOrderRequest(null, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Restaurant ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when restaurant ID is zero")
    void testValidateCreateOrderRequest_ZeroRestaurantId() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 2));
        CreateOrderRequest request = new CreateOrderRequest(0L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Restaurant ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when restaurant ID is negative")
    void testValidateCreateOrderRequest_NegativeRestaurantId() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 2));
        CreateOrderRequest request = new CreateOrderRequest(-5L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Restaurant ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when order items is null")
    void testValidateCreateOrderRequest_NullOrderItems() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(10L, null);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Order must contain at least one item", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when order items is empty")
    void testValidateCreateOrderRequest_EmptyOrderItems() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(10L, List.of());

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Order must contain at least one item", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when menu item ID is null")
    void testValidateOrderItem_NullMenuItemId() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(null, 2));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Menu item ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when menu item ID is zero")
    void testValidateOrderItem_ZeroMenuItemId() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(0L, 2));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Menu item ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when menu item ID is negative")
    void testValidateOrderItem_NegativeMenuItemId() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(-1L, 2));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Menu item ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when quantity is null")
    void testValidateOrderItem_NullQuantity() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, null));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Quantity must be greater than zero", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when quantity is zero")
    void testValidateOrderItem_ZeroQuantity() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 0));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Quantity must be greater than zero", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when quantity is negative")
    void testValidateOrderItem_NegativeQuantity() {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, -5));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Quantity must be greater than zero", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate multiple order items successfully")
    void testValidateCreateOrderRequest_MultipleItems() {
        // Arrange
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 3),
                new OrderItemRequest(3L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateCreateOrderRequest(request));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100})
    @DisplayName("Should validate valid quantities")
    void testValidateOrderItem_ValidQuantities(int quantity) {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, quantity));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateCreateOrderRequest(request));
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 5L, 100L, 999999L})
    @DisplayName("Should validate valid restaurant IDs")
    void testValidateCreateOrderRequest_ValidRestaurantIds(long restaurantId) {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(1L, 2));
        CreateOrderRequest request = new CreateOrderRequest(restaurantId, items);

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateCreateOrderRequest(request));
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 5L, 100L, 999999L})
    @DisplayName("Should validate valid menu item IDs")
    void testValidateOrderItem_ValidMenuItemIds(long menuItemId) {
        // Arrange
        List<OrderItemRequest> items = List.of(new OrderItemRequest(menuItemId, 2));
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateCreateOrderRequest(request));
    }

    @Test
    @DisplayName("Should fail validation on first invalid item in multiple items")
    void testValidateCreateOrderRequest_FirstItemInvalid() {
        // Arrange
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(0L, 2),
                new OrderItemRequest(2L, 3)
        );
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Menu item ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation on middle invalid item in multiple items")
    void testValidateCreateOrderRequest_MiddleItemInvalid() {
        // Arrange
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(-1L, 3),
                new OrderItemRequest(3L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest(10L, items);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validationService.validateCreateOrderRequest(request));
        assertEquals("Menu item ID is required", exception.getMessage());
    }
}
