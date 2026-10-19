package com.aerosaga.backend.repository;

import com.aerosaga.backend.entity.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface DeliveryTaskRepository
        extends JpaRepository<DeliveryTask, Long> {

    boolean existsByDrone_DroneIdAndStatusIn(String droneId, Collection<String> statuses);

    List<DeliveryTask> findByDrone_DroneIdOrderByCreatedAtDesc(String droneId);

    List<DeliveryTask> findByStatusIn(Collection<String> statuses);
}