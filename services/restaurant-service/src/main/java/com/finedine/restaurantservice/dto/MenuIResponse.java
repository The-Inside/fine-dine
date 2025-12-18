package com.finedine.restaurantservice.dto;

public record MenuIResponse (
        Long id,
        String name,
        String description,
        Double price,
        String category,
        Boolean isAvailable,
        Long restaurantId){

}
