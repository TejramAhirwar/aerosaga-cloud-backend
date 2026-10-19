package com.aerosaga.backend.service;

import com.aerosaga.backend.dto.DroneCommandRequest;
import com.aerosaga.backend.dto.DroneTelemetry;
import com.aerosaga.backend.entity.Drone;
import com.aerosaga.backend.repository.DroneRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DroneSimulatorService {

    private static final double GPS_STEP = 0.0005;
    private static final double ALTITUDE_STEP = 5.0;
    private static final double MAX_ALTITUDE = 120.0;

    private final DroneRepository droneRepository;

    private final Map<String, DroneState> drones =
            new ConcurrentHashMap<>();

    public DroneSimulatorService(DroneRepository droneRepository) {
        this.droneRepository = droneRepository;
    }

    public DroneTelemetry executeCommand(
            String droneId,
            DroneCommandRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Command request is required");
        }

        return executeCommand(
                droneId,
                request.getCommand(),
                request.getLatitude(),
                request.getLongitude(),
                request.getAltitude()
        );
    }

    public DroneTelemetry executeCommand(
            String droneId,
            String command,
            Double latitude,
            Double longitude,
            Double altitude) {

        if (droneId == null || droneId.isBlank()) {
            throw new IllegalArgumentException(
                    "Drone ID is required");
        }

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException(
                    "Command is required");
        }

        synchronizeDrones();

        DroneState state = drones.get(droneId);

        if (state == null) {
            throw new IllegalArgumentException(
                    "Drone not found: " + droneId);
        }

        synchronized (state) {
            DroneTelemetry telemetry = state.telemetry;
            String cmd = command.trim().toUpperCase();

            switch (cmd) {

                case "TAKEOFF": {
                    requireStatus(state, "IDLE");

                    if (state.destinationLatitude == null
                            || state.destinationLongitude == null) {
                        throw new IllegalStateException(
                                "Assign a destination before takeoff");
                    }

                    if (telemetry.getBattery() <= 0) {
                        throw new IllegalStateException(
                                "Drone battery is empty");
                    }

                    double targetAltitude =
                            altitude == null ? 30.0 : altitude;

                    validateAltitude(targetAltitude);

                    state.targetLatitude = telemetry.getLatitude();
                    state.targetLongitude = telemetry.getLongitude();
                    state.targetAltitude = targetAltitude;

                    updateStatus(state, "TAKING_OFF");
                    break;
                }

                case "NAVIGATE": {
                    requireStatus(state, "IN_FLIGHT");

                    if (state.destinationLatitude == null
                            || state.destinationLongitude == null) {
                        throw new IllegalStateException(
                                "No destination assigned to this drone");
                    }

                    if (telemetry.getBattery() <= 0) {
                        throw new IllegalStateException(
                                "Drone battery is empty");
                    }

                    // Use the destination saved in the database.
                    state.targetLatitude = state.destinationLatitude;
                    state.targetLongitude = state.destinationLongitude;
                    state.targetAltitude = telemetry.getAltitude();

                    updateStatus(state, "NAVIGATING");
                    break;
                }

                case "DROP": {
                    requireStatus(state, "ARRIVED");

                    if (telemetry.getAltitude() <= 0) {
                        throw new IllegalStateException(
                                "Drone must be airborne to deliver payload");
                    }

                    // Simulation: mark delivery as completed.
                    updateStatus(state, "DELIVERED");
                    break;
                }

                case "RETURN_TO_BASE": {
                    requireStatus(state, "DELIVERED");

                    state.targetLatitude = state.baseLatitude;
                    state.targetLongitude = state.baseLongitude;
                    state.targetAltitude = 0.0;

                    updateStatus(state, "RETURNING");
                    break;
                }
                case "RESET": {
                    String currentStatus = state.telemetry.getStatus();

                    boolean isLanded =
                            "LANDED".equalsIgnoreCase(currentStatus);

                    boolean isGroundedArrived =
                            "ARRIVED".equalsIgnoreCase(currentStatus)
                                    && state.telemetry.getAltitude() <= 0.0
                                    && state.telemetry.getSpeed() <= 0.0;

                    if (!isLanded && !isGroundedArrived) {
                        throw new IllegalStateException(
                                "RESET requires LANDED status or a grounded ARRIVED drone"
                        );
                    }

                    // Reset position to this drone's registered base.
                    state.telemetry.setLatitude(state.baseLatitude);
                    state.telemetry.setLongitude(state.baseLongitude);
                    state.telemetry.setAltitude(0.0);
                    state.telemetry.setSpeed(0.0);
                    state.telemetry.setHeading(0.0);

                    state.targetLatitude = state.baseLatitude;
                    state.targetLongitude = state.baseLongitude;
                    state.targetAltitude = 0.0;

                    // Clear the previous delivery destination in the database.
                    Drone drone = droneRepository.findById(droneId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Drone not found: " + droneId));

                    drone.setDestinationLatitude(null);
                    drone.setDestinationLongitude(null);
                    drone.setStatus("IDLE");
                    droneRepository.save(drone);

                    // Keep simulator memory in sync with the database.
                    state.destinationLatitude = null;
                    state.destinationLongitude = null;
                    state.telemetry.setStatus("IDLE");
                    state.telemetry.setTimestamp(Instant.now());
                    state.lastUpdateTime = Instant.now();

                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            "Unsupported command: " + command);
            }

            telemetry.setTimestamp(Instant.now());
            return telemetry;
        }
    }

    public List<DroneTelemetry> advanceAndGetTelemetry() {
        synchronizeDrones();

        List<DroneTelemetry> result = new ArrayList<>();

        for (DroneState state : drones.values()) {
            synchronized (state) {
                moveDrone(state);
                result.add(state.telemetry);
            }
        }

        return result;
    }

    public DroneTelemetry getDroneState(String droneId) {
        synchronizeDrones();

        DroneState state = drones.get(droneId);

        if (state == null) {
            throw new IllegalArgumentException(
                    "Drone not found: " + droneId);
        }

        synchronized (state) {
            return state.telemetry;
        }
    }

    private void synchronizeDrones() {
        List<Drone> registeredDrones = droneRepository.findAll();

        for (Drone drone : registeredDrones) {
            if (drone.getDroneId() == null
                    || drone.getDroneId().isBlank()) {
                continue;
            }

            drones.compute(drone.getDroneId(), (id, existing) -> {
                if (existing == null) {
                    return new DroneState(drone);
                }

                synchronized (existing) {
                    // Refresh destination and base if database values change.
                    existing.baseLatitude = drone.getBaseLatitude();
                    existing.baseLongitude = drone.getBaseLongitude();
                    existing.destinationLatitude =
                            drone.getDestinationLatitude();
                    existing.destinationLongitude =
                            drone.getDestinationLongitude();
                }

                return existing;
            });
        }
    }

    private void moveDrone(DroneState state) {
        DroneTelemetry telemetry = state.telemetry;
        Instant now = Instant.now();

        String status = telemetry.getStatus();

        if (telemetry.getBattery() <= 0) {
            telemetry.setBattery(0.0);
            telemetry.setSpeed(0.0);
            updateStatus(state, "LOW_BATTERY");
            telemetry.setTimestamp(now);
            state.lastUpdateTime = now;
            return;
        }

        boolean active =
                "TAKING_OFF".equals(status)
                        || "IN_FLIGHT".equals(status)
                        || "NAVIGATING".equals(status)
                        || "RETURNING".equals(status);

        if (!active) {
            telemetry.setSpeed(0.0);

telemetry.setBattery(Math.max(0.0, telemetry.getBattery() - 0.002));

            telemetry.setTimestamp(now);
            state.lastUpdateTime = now;

            if (telemetry.getBattery() <= 0) {
                telemetry.setBattery(0.0);
                updateStatus(state, "LOW_BATTERY");
            }
            return;
        }

        double oldLat = telemetry.getLatitude();
        double oldLon = telemetry.getLongitude();
        double oldAlt = telemetry.getAltitude();

        double newLat =
                moveTowards(oldLat, state.targetLatitude, GPS_STEP);

        double newLon =
                moveTowards(oldLon, state.targetLongitude, GPS_STEP);

        double newAlt =
                moveTowards(oldAlt, state.targetAltitude, ALTITUDE_STEP);

        boolean horizontalMovement =
                oldLat != newLat || oldLon != newLon;

        boolean moving =
                horizontalMovement || oldAlt != newAlt;

        if (horizontalMovement) {
            telemetry.setHeading(
                    calculateHeading(oldLat, oldLon, newLat, newLon));
        }

        double horizontalDistance =
                calculateDistance(oldLat, oldLon, newLat, newLon);

        double altitudeDistance = newAlt - oldAlt;

        double distance3D = Math.sqrt(
                horizontalDistance * horizontalDistance
                        + altitudeDistance * altitudeDistance);

        double elapsedSeconds = Math.max(
                0.001,
                (now.toEpochMilli()
                        - state.lastUpdateTime.toEpochMilli()) / 1000.0);

        telemetry.setSpeed(
                moving ? distance3D / elapsedSeconds : 0.0);

        telemetry.setLatitude(newLat);
        telemetry.setLongitude(newLon);
        telemetry.setAltitude(newAlt);

        telemetry.setBattery(
                Math.max(
                        0.0,
                        telemetry.getBattery()
                                - (moving ? 0.005 : 0.002)));

        boolean atTarget =
                Math.abs(newLat - state.targetLatitude) < 1e-9
                        && Math.abs(newLon - state.targetLongitude) < 1e-9
                        && Math.abs(newAlt - state.targetAltitude) < 1e-9;

        if (telemetry.getBattery() <= 0) {
            telemetry.setBattery(0.0);
            telemetry.setSpeed(0.0);
            updateStatus(state, "LOW_BATTERY");
        } else if (atTarget) {
            switch (telemetry.getStatus()) {
                case "TAKING_OFF":
                    updateStatus(state, "IN_FLIGHT");
                    break;

                case "NAVIGATING":
                    updateStatus(state, "ARRIVED");
                    break;

                case "RETURNING":
                    updateStatus(state, "LANDED");
                    break;

                default:
                    break;
            }
        }

        telemetry.setTimestamp(now);
        state.lastUpdateTime = now;
    }

    private void requireStatus(DroneState state, String requiredStatus) {
        String currentStatus = state.telemetry.getStatus();

        if (!requiredStatus.equals(currentStatus)) {
            throw new IllegalStateException(
                    "Command requires status " + requiredStatus
                            + ", but current status is " + currentStatus);
        }
    }

    private void updateStatus(DroneState state, String newStatus) {
        String currentStatus = state.telemetry.getStatus();

        if (newStatus.equals(currentStatus)) {
            return;
        }

        Drone drone = droneRepository.findById(
                state.telemetry.getDroneId()
        ).orElseThrow(() -> new IllegalArgumentException(
                "Drone not found: " + state.telemetry.getDroneId()));

        drone.setStatus(newStatus);
        droneRepository.save(drone);

        state.telemetry.setStatus(newStatus);
    }

    private double moveTowards(
            double current, double target, double step) {

        if (Math.abs(target - current) <= step) {
            return target;
        }

        return current + Math.copySign(step, target - current);
    }

    private double calculateHeading(
            double lat1, double lon1,
            double lat2, double lon2) {

        double dLat = lat2 - lat1;
        double dLon = (lon2 - lon1)
                * Math.cos(Math.toRadians(lat1));

        double heading = Math.toDegrees(Math.atan2(dLon, dLat));

        return (heading + 360.0) % 360.0;
    }

    private double calculateDistance(
            double lat1, double lon1,
            double lat2, double lon2) {

        final double EARTH_RADIUS_METRES = 6_371_000.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(radLat1) * Math.cos(radLat2)
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(Math.max(0.0, 1.0 - a)));

        return EARTH_RADIUS_METRES * c;
    }

    private void validateCoordinates(
            double latitude, double longitude) {

        if (!Double.isFinite(latitude)
                || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90");
        }

        if (!Double.isFinite(longitude)
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180");
        }
    }

    private void validateAltitude(double altitude) {
        if (!Double.isFinite(altitude)
                || altitude <= 0 || altitude > MAX_ALTITUDE) {
            throw new IllegalArgumentException(
                    "Altitude must be greater than 0 and at most 120");
        }
    }

    private static class DroneState {
        private final DroneTelemetry telemetry;

        private Instant lastUpdateTime = Instant.now();

        private double baseLatitude;
        private double baseLongitude;

        private Double destinationLatitude;
        private Double destinationLongitude;

        private double targetLatitude;
        private double targetLongitude;
        private double targetAltitude;

        private DroneState(Drone drone) {
            this.baseLatitude = drone.getBaseLatitude();
            this.baseLongitude = drone.getBaseLongitude();

            this.destinationLatitude = drone.getDestinationLatitude();
            this.destinationLongitude = drone.getDestinationLongitude();

            this.telemetry = new DroneTelemetry();
            this.telemetry.setDroneId(drone.getDroneId());
            this.telemetry.setLatitude(baseLatitude);
            this.telemetry.setLongitude(baseLongitude);
            this.telemetry.setAltitude(0.0);
            this.telemetry.setBattery(100.0);
            this.telemetry.setSpeed(0.0);
            this.telemetry.setHeading(0.0);
            this.telemetry.setMissionId(null);
            this.telemetry.setStatus(
                    drone.getStatus() == null ? "IDLE" : drone.getStatus());
            this.telemetry.setTimestamp(Instant.now());

            this.targetLatitude = baseLatitude;
            this.targetLongitude = baseLongitude;
            this.targetAltitude = 0.0;
        }
    }
}