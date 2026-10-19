package com.aerosaga.backend.service;

import com.aerosaga.backend.dto.DroneDashboardMessage;
import com.aerosaga.backend.dto.DroneDashboardMessage.DroneSummary;
import com.aerosaga.backend.dto.DroneDashboardMessage.TaskSummary;
import com.aerosaga.backend.entity.Drone;
import com.aerosaga.backend.entity.DeliveryTask;
import com.aerosaga.backend.repository.DroneRepository;
import com.aerosaga.backend.repository.DeliveryTaskRepository;
import com.aerosaga.backend.websocket.TelemetryWebSocketHandler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

@Service
public class DashboardBroadcastService {

    private static final Set<String> ACTIVE_TASKS =
            Set.of("ASSIGNED", "IN_PROGRESS");

    private final DroneRepository droneRepository;
    private final DeliveryTaskRepository taskRepository;
    private final TelemetryWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public DashboardBroadcastService(
            DroneRepository droneRepository,
            DeliveryTaskRepository taskRepository,
            TelemetryWebSocketHandler webSocketHandler,
            ObjectMapper objectMapper) {
        this.droneRepository = droneRepository;
        this.taskRepository = taskRepository;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 3000)
    @Transactional(readOnly = true)
    public void broadcastDashboard() {
        try {
            List<Drone> allDrones = droneRepository.findAll();

            List<DroneSummary> drones = allDrones.stream()
                    .map(d -> new DroneSummary(
                            d.getDroneId(),
                            d.getBaseLatitude(),
                            d.getBaseLongitude(),
                            d.getStatus(),
                            d.getCreatedAt()
                    ))
                    .toList();

            List<TaskSummary> activeTasks =
                    taskRepository.findByStatusIn(ACTIVE_TASKS)
                            .stream()
                            .map(t -> new TaskSummary(
                                    t.getTaskId(),
                                    t.getDrone().getDroneId(),
                                    t.getDestinationLatitude(),
                                    t.getDestinationLongitude(),
                                    t.getDeliveryAddress(),
                                    t.getStatus(),
                                    t.getUpdatedAt()
                            ))
                            .toList();

            long idle = allDrones.stream()
                    .filter(d -> "IDLE".equals(d.getStatus()))
                    .count();

            long active = allDrones.stream()
                    .filter(d -> "ACTIVE".equals(d.getStatus()))
                    .count();

            DroneDashboardMessage message =
                    new DroneDashboardMessage(
                            "DRONE_DASHBOARD_UPDATE",
                            allDrones.size(),
                            idle,
                            active,
                            drones,
                            activeTasks
                    );

            String json = objectMapper.writeValueAsString(message);
            webSocketHandler.broadcast(json);

        } catch (Exception e) {
            System.err.println(
                    "Dashboard broadcast failed: " + e.getMessage()
            );
        }
    }
}