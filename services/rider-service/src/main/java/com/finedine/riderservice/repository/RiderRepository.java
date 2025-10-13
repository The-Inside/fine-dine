package com.finedine.riderservice.repository;

import com.finedine.riderservice.dto.RiderResponse;
import com.finedine.riderservice.entity.Rider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByExternalId(String externalId);

    @Query("SELECT r FROM Rider r WHERE r.status = 'ONLINE' and r.availability = 'AVAILABLE'")
    List<Rider> findByAvailable();

    @Query("SELECT r FROM Rider r WHERE r.status = 'ONLINE' and r.availability = 'AVAILABLE'")
    Page<Rider> findOnlineAndAvailableRiders(Pageable pageable);
}
