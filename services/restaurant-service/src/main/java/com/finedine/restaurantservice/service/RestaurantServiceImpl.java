package com.finedine.restaurantservice.service;

import com.finedine.restaurantservice.dto.*;
import com.finedine.restaurantservice.entity.MenuItem;
import com.finedine.restaurantservice.entity.Restaurant;
import com.finedine.common.exception.NotFoundException;
import com.finedine.common.exception.UnauthorizedException;
import com.finedine.restaurantservice.repository.MenuRepository;
import com.finedine.restaurantservice.repository.RestaurantRepository;
import com.finedine.restaurantservice.security.SecurityUser;
import com.finedine.restaurantservice.util.RestaurantMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

import static com.finedine.restaurantservice.util.CustomMessages.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService{

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final MenuRepository menuRepository;
    private static final double AVERAGE_SPEED_KMH = 30.0;

    /**
     {@inheritDoc}
     */
    @SqsListener(value = "fds-restaurant-registration-queue.fifo")
    @Transactional
    @Override
    public void createRestaurantEntry(RestaurantQueueObject data) {
        String restaurantCode;

        do {
            restaurantCode = generateRestaurantCode();
        } while (!isRestaurantCodeUnique(restaurantCode));

        log.info("Received new restaurant data: {}", data);

        Restaurant restaurant = restaurantMapper.toRestaurant(data);

        restaurant.setRestaurantCode(restaurantCode);

        var savedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant saved successfully with ID: {}, externalId: {}, name: {}",
                savedRestaurant.getRestaurantId(),
                savedRestaurant.getExternalId(),
                savedRestaurant.getRestaurantName());
    }

    /**
     {@inheritDoc}
     */
    @Override
    public RestaurantResponse myRestaurant(SecurityUser securityUser) {

        Restaurant restaurant = findByExternalId(securityUser);

        validateTenant(securityUser, restaurant);

        return restaurantMapper.toRestaurantResponse(restaurant);

    }

    /**
     {@inheritDoc}
     */
    @Override
    public Page<RestaurantResponse> allRestaurants(Pageable pageable) {
        Page<Restaurant> restaurants = restaurantRepository.findAll(pageable);
        return restaurants.map(restaurantMapper::toRestaurantResponse);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public Page<RestaurantResponse> nearByRestaurants(Pageable pageable, double userLat, double userLon, double radiusKm) {

        return restaurantRepository.findAllByDeletedAtIsNullAndWithinRadius(userLat, userLon, radiusKm, pageable)
                .map(restaurant -> {
                    double distanceKm = calculateDistance(userLat, userLon, restaurant.getLatitude(), restaurant.getLongitude());
                    int travelTimeMinutes = (int) Math.round((distanceKm / AVERAGE_SPEED_KMH) * 60);
                    return new RestaurantResponse(
                            restaurant.getRestaurantId(),
                            restaurant.getExternalId(),
                            restaurant.getEmail(),
                            restaurant.getRestaurantName(),
                            restaurant.getRestaurantCode(),
                            restaurant.getPhone(),
                            restaurant.getLogoUrl(),
                            restaurant.getAddress(),
                            restaurant.getLatitude(),
                            restaurant.getLongitude(),
                            restaurant.getCuisine(),
                            restaurant.getDescription(),
                            travelTimeMinutes,
                            restaurantMapper.isRestaurantOpen(restaurant.getOpenTime(), restaurant.getCloseTime())
                    );

                });
    }

    /**
     {@inheritDoc}
     */
    @Override
    public RestaurantResponse profileSettings(SecurityUser securityUser, RestaurantUpdateRequest request) {

        Restaurant restaurant = findByExternalId(securityUser);

        validateTenant(securityUser, restaurant);

        restaurantMapper.updateRestaurantFromRequest(request, restaurant);
        restaurantRepository.save(restaurant);

        return restaurantMapper.toRestaurantResponse(restaurant);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public GenericMessageResponse addMenuItem(SecurityUser securityUser, MenuItemRequest request) {
        Restaurant restaurant = findByExternalId(securityUser);

        validateTenant(securityUser, restaurant);

        MenuItem menuItem = MenuItem.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .isAvailable(request.isAvailable())
                .restaurant(restaurant)
                .build();

        menuRepository.save(menuItem);
        return new GenericMessageResponse(MENU_ITEM_ADDED_SUCCESS);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public GenericMessageResponse updateMenuItem(SecurityUser securityUser, Long menuItemId, MenuItemRequest request) {
        Restaurant restaurant = findByExternalId(securityUser);

        validateTenant(securityUser, restaurant);

        MenuItem menuItem = menuRepository.findByIdAndRestaurantId(menuItemId, restaurant.getRestaurantId())
                .orElseThrow(() -> new NotFoundException(MENU_ITEM_NOT_FOUND));

        menuItem.setName(request.name());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setCategory(request.category());
        menuItem.setIsAvailable(request.isAvailable());

        menuRepository.save(menuItem);
        return new GenericMessageResponse(MENU_ITEM_UPDATED_SUCCESS);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public GenericMessageResponse deleteMenuItem(SecurityUser securityUser, Long menuItemId) {
        Restaurant restaurant = findByExternalId(securityUser);

        validateTenant(securityUser, restaurant);

        MenuItem menuItem = menuRepository.findByIdAndRestaurantId(menuItemId, restaurant.getRestaurantId())
                .orElseThrow(() -> new NotFoundException(MENU_ITEM_NOT_FOUND));

        menuRepository.delete(menuItem);
        return new GenericMessageResponse(MENU_ITEM_DELETED_SUCCESS);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public Page<MenuItemResponse> getRestaurantMenu(Long restaurantId, Pageable pageable) {
        return menuRepository.findByRestaurantId(restaurantId, pageable);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public List<MenuIResponse> getMenuItemsByIds(List<Long> ids) {
        List<MenuItem> response = menuRepository.findByIdIn(ids);
        log.info("Fetched {} menu items for provided IDs", response);

        return response.stream()
                .map(menuItem -> new MenuIResponse(
                        menuItem.getId(),
                        menuItem.getName(),
                        menuItem.getDescription(),
                        menuItem.getPrice(),
                        menuItem.getCategory(),
                        menuItem.getIsAvailable(),
                        menuItem.getRestaurant().getRestaurantId()))
                .toList();
    }

    private Restaurant findByExternalId(SecurityUser securityUser){
        return restaurantRepository.findByExternalId(securityUser.externalId())
                .orElseThrow(() -> new NotFoundException(RESTAURANT_NOT_FOUND));
    }

    private String generateRestaurantCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        return code.toString();
    }

    private boolean isRestaurantCodeUnique(String code) {
        return !restaurantRepository.existsByRestaurantCode(code);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void validateTenant(SecurityUser securityUser, Restaurant restaurant) {
        if (securityUser.externalId() != null && !securityUser.externalId().equals(restaurant.getExternalId())) {
            throw new UnauthorizedException(RESTRICTED_ACTION);
        }
    }
}
