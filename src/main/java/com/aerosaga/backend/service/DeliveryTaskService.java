
package com.aerosaga.backend.service;

import com.aerosaga.backend.dto.CreateTaskRequest;
import com.aerosaga.backend.dto.TaskResponse;
import com.aerosaga.backend.dto.UpdateTaskStatusRequest;
import com.aerosaga.backend.entity.DeliveryTask;
import com.aerosaga.backend.entity.Drone;
import com.aerosaga.backend.repository.DeliveryTaskRepository;
import com.aerosaga.backend.repository.DroneRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class DeliveryTaskService {

    private static final Set<String> ACTIVE_TASK_STATUSES =
            Set.of("ASSIGNED", "IN_PROGRESS");

    private static final Set<String> ALLOWED_NEXT_STATUSES =
            Set.of("IN_PROGRESS", "COMPLETED", "CANCELLED");

    private final DroneRepository droneRepository;
    private final DeliveryTaskRepository taskRepository;

    public DeliveryTaskService(
            DroneRepository droneRepository,
            DeliveryTaskRepository taskRepository) {
        this.droneRepository = droneRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse assignTask(
            String droneId, CreateTaskRequest request) {

        if (request == null
                || request.destinationLatitude() == null
                || request.destinationLongitude() == null
                || request.deliveryAddress() == null
                || request.deliveryAddress().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Destination latitude, longitude and delivery address are required"
            );
        }

        double latitude = request.destinationLatitude();
        double longitude = request.destinationLongitude();

        if (!Double.isFinite(latitude)
                || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude)
                || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid destination coordinates"
            );
        }

        Drone drone = droneRepository.findByIdForUpdate(droneId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Drone not found"
                ));

        if (!"IDLE".equals(drone.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Drone is not idle and cannot accept a task"
            );
        }

        boolean hasActiveTask =
                taskRepository.existsByDrone_DroneIdAndStatusIn(
                        droneId, ACTIVE_TASK_STATUSES
                );

        if (hasActiveTask) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Drone already has an active task"
            );
        }

        Instant now = Instant.now();

        DeliveryTask task = new DeliveryTask();
        task.setDrone(drone);
        task.setDestinationLatitude(latitude);
        task.setDestinationLongitude(longitude);
        task.setDeliveryAddress(request.deliveryAddress().trim());
        task.setStatus("ASSIGNED");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        drone.setStatus("ACTIVE");

        DeliveryTask saved = taskRepository.save(task);

        return TaskResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getDroneTasks(String droneId) {

        if (!droneRepository.existsById(droneId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Drone not found"
            );
        }

        return taskRepository
                .findByDrone_DroneIdOrderByCreatedAtDesc(droneId)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            String droneId,
            Long taskId,
            UpdateTaskStatusRequest request) {

        DeliveryTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"
                ));

        if (task.getDrone() == null
                || !task.getDrone().getDroneId().equals(droneId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Task not found for this drone"
            );
        }

        if (request == null || request.status() == null
                || request.status().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Task status is required"
            );
        }

        String next = request.status().trim().toUpperCase();
        String current = task.getStatus();

        if (!ALLOWED_NEXT_STATUSES.contains(next)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid task status"
            );
        }

        if (!ACTIVE_TASK_STATUSES.contains(current)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Task is already finished"
            );
        }

        boolean validTransition =
                ("ASSIGNED".equals(current)
                        && ("IN_PROGRESS".equals(next)
                        || "CANCELLED".equals(next)))
                        || ("IN_PROGRESS".equals(current)
                        && ("COMPLETED".equals(next)
                        || "CANCELLED".equals(next)));

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Invalid task status transition"
            );
        }

        task.setStatus(next);
        task.setUpdatedAt(Instant.now());

        if ("COMPLETED".equals(next) || "CANCELLED".equals(next)) {
            task.getDrone().setStatus("IDLE");
        }

        return TaskResponse.from(task);
    }
}
