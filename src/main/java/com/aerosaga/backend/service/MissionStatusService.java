
package com.aerosaga.backend.service;

import com.aerosaga.DroneMissionWorkflow;
import com.aerosaga.backend.websocket.TelemetryWebSocketHandler;
import io.temporal.client.WorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MissionStatusService {

    private static final Logger log =
            LoggerFactory.getLogger(MissionStatusService.class);

    private final WorkflowClient workflowClient;
    private final TelemetryWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Value("${temporal.workflow-id:}")
    private String workflowId;

    @Value("${temporal.drone-id:}")
    private String droneId;

    private volatile String lastStatus;

    public MissionStatusService(
            WorkflowClient workflowClient,
            TelemetryWebSocketHandler webSocketHandler,
            ObjectMapper objectMapper) {
        this.workflowClient = workflowClient;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Scheduled(
            fixedDelayString =
                    "${temporal.status.poll-interval-ms:2000}"
    )
    public void pollMissionStatus() {

        if (workflowId == null || workflowId.isBlank()
                || droneId == null || droneId.isBlank()) {
            return;
        }

        try {
            DroneMissionWorkflow workflow =
                    workflowClient.newWorkflowStub(
                            DroneMissionWorkflow.class,
                            workflowId
                    );

            String currentStatus = workflow.getMissionStatus();

            if (currentStatus == null || currentStatus.isBlank()) {
                log.warn(
                        "Empty mission status for workflow {}",
                        workflowId
                );
                return;
            }

            if (!currentStatus.equals(lastStatus)) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("droneId", droneId);
                data.put("workflowId", workflowId);
                data.put("missionStatus", currentStatus);
                data.put("timestamp", Instant.now().toString());

                Map<String, Object> message = new LinkedHashMap<>();
                message.put("type", "MISSION_STATUS_UPDATE");
                message.put("data", data);

                String json = objectMapper.writeValueAsString(message);
                webSocketHandler.broadcast(json);

                lastStatus = currentStatus;

                log.info(
                        "Mission status broadcast: droneId={}, workflowId={}, status={}",
                        droneId, workflowId, currentStatus
                );
            }

        } catch (Exception e) {
            log.warn(
                    "Unable to query or broadcast workflow {}: {}",
                    workflowId,
                    e.getMessage()
            );
        }
    }
}
