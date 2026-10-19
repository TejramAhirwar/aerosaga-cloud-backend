
package com.aerosaga.backend.controller;

import com.aerosaga.backend.dto.DroneTelemetry;
import com.aerosaga.backend.service.TelemetryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    // Get all drones
    @GetMapping
    public List<DroneTelemetry> getTelemetry() {
        return telemetryService.getRegisteredDroneTelemetry();
    }

    // Get one drone by ID
    @GetMapping("/{droneId}")
    public ResponseEntity<DroneTelemetry> getTelemetryByDroneId(@PathVariable String droneId) {

        return telemetryService
                .getTelemetryByDroneId(droneId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
