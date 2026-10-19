
package com.aerosaga.backend.controller;

import com.aerosaga.backend.dto.DestinationRequest;
import com.aerosaga.backend.dto.DroneCommandRequest;
import com.aerosaga.backend.dto.DroneTelemetry;
import com.aerosaga.backend.entity.Drone;
import com.aerosaga.backend.service.DroneDestinationService;
import com.aerosaga.backend.service.DroneSimulatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator/drones")
public class SimulatorController {

    private final DroneSimulatorService simulatorService;
    private final DroneDestinationService destinationService;

    public SimulatorController(DroneSimulatorService simulatorService,  DroneDestinationService destinationService) {
        this.simulatorService = simulatorService;
        this.destinationService=destinationService;
    }

    @PostMapping("/{droneId}/commands")
    public ResponseEntity<?> executeCommand(
            @PathVariable String droneId,
            @RequestBody DroneCommandRequest request) {
        try {
            DroneTelemetry result =
                    simulatorService.executeCommand(droneId, request);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{droneId}/state")
    public ResponseEntity<?> getState(
            @PathVariable String droneId) {
        try {
            return ResponseEntity.ok(
                    simulatorService.getDroneState(droneId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{droneId}/destination")
    public ResponseEntity<?> assignDestination(
            @PathVariable String droneId,
            @RequestBody DestinationRequest request) {

        try {
            Drone drone =
                    destinationService.assignDestination(droneId, request);

            return ResponseEntity.ok(Map.of(
                    "message", "Destination assigned successfully",
                    "droneId", drone.getDroneId(),
                    "destinationLatitude", drone.getDestinationLatitude(),
                    "destinationLongitude", drone.getDestinationLongitude(),
                    "status", drone.getStatus()
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }


}
