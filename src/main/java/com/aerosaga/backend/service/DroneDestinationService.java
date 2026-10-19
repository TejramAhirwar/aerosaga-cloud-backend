
package com.aerosaga.backend.service;

import com.aerosaga.backend.dto.DestinationRequest;
import com.aerosaga.backend.entity.Drone;
import com.aerosaga.backend.repository.DroneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DroneDestinationService {

    private final DroneRepository droneRepository;

    public DroneDestinationService(DroneRepository droneRepository) {
        this.droneRepository = droneRepository;
    }

    @Transactional
    public Drone assignDestination(
            String droneId,
            DestinationRequest request) {

        if (request == null
                || request.getLatitude() == null
                || request.getLongitude() == null) {
            throw new IllegalArgumentException(
                    "Destination latitude and longitude are required");
        }

        double latitude = request.getLatitude();
        double longitude = request.getLongitude();

        if (!Double.isFinite(latitude)
                || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90");
        }

        if (!Double.isFinite(longitude)
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180");
        }

        Drone drone = droneRepository.findByIdForUpdate(droneId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Drone not found: " + droneId));

        if (!"IDLE".equalsIgnoreCase(drone.getStatus())) {
            throw new IllegalStateException(
                    "Destination can only be assigned when drone is IDLE");
        }

        drone.setDestinationLatitude(latitude);
        drone.setDestinationLongitude(longitude);

        return droneRepository.save(drone);
    }
}
