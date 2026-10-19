
package com.aerosaga.backend.service;

import com.aerosaga.backend.dto.CreateDroneRequest;
import com.aerosaga.backend.dto.DroneResponse;
import com.aerosaga.backend.entity.Drone;
import com.aerosaga.backend.repository.DroneRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class DroneService {

    private final DroneRepository repository;

    public DroneService(DroneRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DroneResponse addDrone(CreateDroneRequest request) {

        if (request == null
                || request.droneId() == null
                || request.droneId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Drone ID is required"
            );
        }

        String id = request.droneId().trim();

        if (repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Drone ID already exists"
            );
        }

        Drone drone = new Drone();
        drone.setDroneId(id);
        drone.setBaseLatitude(request.baseLatitude());
        drone.setBaseLongitude(request.baseLongitude());
        drone.setStatus("IDLE");
        drone.setCreatedAt(Instant.now());

        Drone savedDrone = repository.save(drone);

        return DroneResponse.from(savedDrone);
    }

    @Transactional(readOnly = true)
    public List<DroneResponse> getAllDrones() {
        return repository.findAll()
                .stream()
                .map(DroneResponse::from)
                .toList();
    }
}
