package com.finedine.restaurantservice.repository;

import com.finedine.restaurantservice.dto.MenuItemResponse;
import com.finedine.restaurantservice.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<MenuItem, Long> {
    @Query("SELECT m FROM MenuItem m WHERE m.id = :id AND m.restaurant.restaurantId = :restaurantId")
    Optional<MenuItem> findByIdAndRestaurantId(@Param("id") Long id, @Param("restaurantId") Long restaurantId);

    @Query("SELECT m.name AS name, m.description as description, m.price AS price, m.category as category, m.isAvailable as available " +
            "FROM MenuItem m WHERE m.restaurant.restaurantId = :restaurantId")
    Page<MenuItemResponse> findByRestaurantId(Long restaurantId, Pageable pageable);

    @Query("SELECT m FROM MenuItem m WHERE m.id IN :ids")
    List<MenuItem> findByIdIn(List<Long> ids);

}
