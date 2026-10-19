package com.aerosaga.backend.controller;

import com.aerosaga.backend.dto.*;
import com.aerosaga.backend.service.DroneService;
import com.aerosaga.backend.service.DeliveryTaskService;
import com.aerosaga.backend.service.DashboardBroadcastService;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drones")
public class DroneController {

    private final DroneService droneService;
    private final DeliveryTaskService taskService;
    private final DashboardBroadcastService dashboard;

    public DroneController(
            DroneService droneService,
            DeliveryTaskService taskService,
            DashboardBroadcastService dashboard) {
        this.droneService = droneService;
        this.taskService = taskService;
        this.dashboard = dashboard;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DroneResponse addDrone(
             @RequestBody CreateDroneRequest request) {
        DroneResponse response = droneService.addDrone(request);
        dashboard.broadcastDashboard();
        return response;
    }

    @GetMapping
    public List<DroneResponse> getAllDrones() {
        return droneService.getAllDrones();
    }

    @PostMapping("/{droneId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse assignTask(
            @PathVariable String droneId,
          @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response =
                taskService.assignTask(droneId, request);
        dashboard.broadcastDashboard();
        return response;
    }

    @GetMapping("/{droneId}/tasks")
    public List<TaskResponse> getDroneTasks(
            @PathVariable String droneId) {
        return taskService.getDroneTasks(droneId);
    }

    @PatchMapping("/{droneId}/tasks/{taskId}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable String droneId,
            @PathVariable Long taskId,
             @RequestBody UpdateTaskStatusRequest request) {
        TaskResponse response =
                taskService.updateTaskStatus(droneId, taskId, request);
        dashboard.broadcastDashboard();
        return response;
    }
}