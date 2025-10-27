package com.finedine.riderservice.repository;

import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(Long orderId);

    List<Delivery> findByRiderIdAndStatusNot(Long riderId, DeliveryStatus status);

    @Query("SELECT d FROM Delivery d WHERE d.riderId = :riderId and d.status = 'REQUESTED'")
    Page<Delivery> findByRiderIdAndStatus(@Param("riderId") Long riderId, Pageable pageable);

    @Query("SELECT d FROM Delivery d WHERE d.status = :status")
    Page<Delivery> findByStatus(@Param("status") DeliveryStatus status, Pageable pageable);
}
