package com.aerosaga.backend.dto;

import com.aerosaga.backend.entity.Drone;
import java.time.Instant;

public record DroneResponse(
        String droneId,
        double baseLatitude,
        double baseLongitude,
        String status,
        Instant createdAt
) {
    public static DroneResponse from(Drone d) {
        return new DroneResponse(
                d.getDroneId(),
                d.getBaseLatitude(),
                d.getBaseLongitude(),
                d.getStatus(),
                d.getCreatedAt()
        );
    }
}
