package com.aerosaga.backend.repository;

import com.aerosaga.backend.entity.Drone;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DroneRepository
        extends JpaRepository<Drone, String> {

    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Drone d where d.droneId = :id")
    Optional<Drone> findByIdForUpdate(@Param("id") String droneId);
}