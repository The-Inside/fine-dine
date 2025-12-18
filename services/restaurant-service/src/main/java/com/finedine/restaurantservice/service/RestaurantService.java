package com.finedine.restaurantservice.service;

import com.finedine.restaurantservice.dto.*;
import com.finedine.restaurantservice.security.SecurityUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RestaurantService {

    /**
     * Listens to the SQS queue for new restaurant registration data and creates a new restaurant entry in the database.
     *
     * @param data The restaurant registration data received from the SQS queue.
     */
    void createRestaurantEntry(RestaurantQueueObject data);

    /**
     * Retrieves the restaurant information associated with the authenticated user.
     *
     * @param securityUser The authenticated user's security details.
     * @return The restaurant information.
     */
    RestaurantResponse myRestaurant(SecurityUser securityUser);

    /**
     * Retrieves a paginated list of all restaurants.
     *
     * @param pageable Pagination information.
     * @return A paginated list of all restaurants.
     */
    Page<RestaurantResponse> allRestaurants(Pageable pageable);

    /**
     * Retrieves a paginated list of restaurants near the user's location within a specified radius.
     *
     * @param pageable Pagination information.
     * @param userLat  The latitude of the user's location.
     * @param userLon  The longitude of the user's location.
     * @param radiusKm The search radius in kilometers.
     * @return A paginated list of nearby restaurants.
     */
    Page<RestaurantResponse> nearByRestaurants( Pageable pageable, double userLat, double userLon, double radiusKm);

    /**
     * Updates the profile settings of the restaurant associated with the authenticated user.
     *
     * @param securityUser The authenticated user's security details.
     * @param request      The restaurant update request containing the new profile settings.
     * @return The updated restaurant information.
     */
    RestaurantResponse profileSettings(SecurityUser securityUser, RestaurantUpdateRequest request);

    /**
     * Adds a new menu item to the restaurant associated with the authenticated user.
     *
     * @param securityUser The authenticated user's security details.
     * @param request      The menu item request containing the details of the new menu item.
     * @return A response indicating the success or failure of the operation.
     */
    GenericMessageResponse addMenuItem(SecurityUser securityUser, MenuItemRequest request);

    /**
     * Updates an existing menu item of the restaurant associated with the authenticated user.
     *
     * @param securityUser The authenticated user's security details.
     * @param menuItemId   The ID of the menu item to be updated.
     * @param request      The menu item request containing the updated details of the menu item.
     * @return A response indicating the success or failure of the operation.
     */
    GenericMessageResponse updateMenuItem(SecurityUser securityUser, Long menuItemId, MenuItemRequest request);

    /**
     * Deletes a menu item from the restaurant associated with the authenticated user.
     *
     * @param securityUser The authenticated user's security details.
     * @param menuItemId   The ID of the menu item to be deleted.
     * @return A response indicating the success or failure of the operation.
     */
    GenericMessageResponse deleteMenuItem(SecurityUser securityUser, Long menuItemId);

    /**
     * Retrieves a paginated list of menu items for a specific restaurant.
     *
     * @param restaurantId The ID of the restaurant whose menu items are to be retrieved.
     * @param pageable     Pagination information.
     * @return A paginated list of menu items for the specified restaurant.
     */
    Page<MenuItemResponse> getRestaurantMenu(Long restaurantId, Pageable pageable);

    /**
     * Retrieves a list of menu items by their IDs.
     *
     * @param ids The list of menu item IDs.
     * @return A list of menu item responses.
     */
    List<MenuIResponse> getMenuItemsByIds(List<Long> ids);

}
