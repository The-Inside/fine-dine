package com.finedine.restaurantservice.util;

import com.finedine.restaurantservice.dto.*;
import com.finedine.restaurantservice.entity.MenuItem;
import com.finedine.restaurantservice.entity.Restaurant;
import org.mapstruct.*;

import java.time.LocalTime;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {
    @Mapping(target = "isOpen", expression = "java(isRestaurantOpen(restaurant.getOpenTime(), restaurant.getCloseTime()))")
    @Mapping(target = "travelTimeMinutes", ignore = true)
    RestaurantResponse toRestaurantResponse(Restaurant restaurant);

    Restaurant toRestaurant(RestaurantQueueObject data);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRestaurantFromRequest(RestaurantUpdateRequest request, @MappingTarget Restaurant restaurant);

    default boolean isRestaurantOpen(LocalTime openTime, LocalTime closeTime) {
        if (openTime == null || closeTime == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        if (closeTime.isBefore(openTime)) {
            return !now.isBefore(openTime) || !now.isAfter(closeTime);
        }
        return !now.isBefore(openTime) && !now.isAfter(closeTime);
    }

//    @Mapping(source = "isAvailable", target = "available")
//    @Mapping(source = "restaurant.restaurantId", target = "restaurantId")
//    MenuIResponse toMenuItemResponse(MenuItem menuItem);
}
