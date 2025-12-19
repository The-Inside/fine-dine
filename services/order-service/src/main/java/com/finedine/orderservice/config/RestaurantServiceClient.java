package com.finedine.orderservice.config;

import com.finedine.orderservice.dto.MenuItemDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange(url = "http://localhost:8085/api/v1/restaurants")
public interface RestaurantServiceClient {

    @GetExchange("/internal/menu-items")
    List<MenuItemDTO> getMenuItemsByIds(@RequestParam("ids") List<Long> ids);
}