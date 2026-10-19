package com.aerosaga.backend.dto;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DroneTelemetry {
    private String droneId;
    private double latitude;
    private double longitude;

    private double battery;
    private String status;
    private double altitude;
    private double speed;
    private double heading;
    private String missionId;
    private Instant timestamp;
}
