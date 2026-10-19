package com.aerosaga.backend.dto;


import java.time.Instant;
import java.util.List;

public record DroneDashboardMessage(
        String type,
        long totalDrones,
        long idleDrones,
        long activeDrones,
        List<DroneSummary> drones,
        List<TaskSummary> activeTasks
) {
    public record DroneSummary(
            String droneId,
            double baseLatitude,
            double baseLongitude,
            String status,
            Instant createdAt
    ) {}

    public record TaskSummary(
            Long taskId,
            String droneId,
            double destinationLatitude,
            double destinationLongitude,
            String deliveryAddress,
            String status,
            Instant updatedAt
    ) {}
}