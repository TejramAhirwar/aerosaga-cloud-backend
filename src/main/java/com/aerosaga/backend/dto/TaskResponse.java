package com.aerosaga.backend.dto;

import com.aerosaga.backend.entity.DeliveryTask;
import java.time.Instant;

public record TaskResponse(
        Long taskId,
        String droneId,
        double destinationLatitude,
        double destinationLongitude,
        String deliveryAddress,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(DeliveryTask t) {
        return new TaskResponse(
                t.getTaskId(),
                t.getDrone().getDroneId(),
                t.getDestinationLatitude(),
                t.getDestinationLongitude(),
                t.getDeliveryAddress(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}