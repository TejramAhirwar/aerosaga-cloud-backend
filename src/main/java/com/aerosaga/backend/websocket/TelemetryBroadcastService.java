
package com.aerosaga.backend.websocket;

import com.aerosaga.backend.dto.DroneTelemetry;
import com.aerosaga.backend.service.DroneSimulatorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class TelemetryBroadcastService {

    private final DroneSimulatorService simulatorService;
    private final TelemetryWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public TelemetryBroadcastService(
            DroneSimulatorService simulatorService,
            TelemetryWebSocketHandler webSocketHandler,
            ObjectMapper objectMapper) {
        this.simulatorService = simulatorService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastTelemetry() {
        try {
            List<DroneTelemetry> drones =
                    simulatorService.advanceAndGetTelemetry();

            for (DroneTelemetry drone : drones) {
                drone.setBattery(
                        roundToTwoDecimals(drone.getBattery()));

                drone.setSpeed(
                        roundToTwoDecimals(drone.getSpeed()));

                drone.setHeading(
                        roundToTwoDecimals(drone.getHeading()));
            }

            String json = objectMapper.writeValueAsString(
                    Map.of(
                            "type", "TELEMETRY_UPDATE",
                            "data", drones
                    )
            );

            webSocketHandler.broadcast(json);

        } catch (Exception e) {
            System.err.println(
                    "Telemetry broadcast failed: " + e.getMessage());
        }
    }
}
