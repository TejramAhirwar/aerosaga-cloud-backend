//
//package com.aerosaga.backend.service;
//
//import com.aerosaga.backend.dto.DroneTelemetry;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//
//@Service
//public class TelemetryService {
//
//
//
//    public List<DroneTelemetry> getMockTelemetry() {
//        return List.of(
//                createTelemetry(
//                        "DRONE-101", 23.2599, 77.4126,
//                        89.0, "FLYING",
//                        120.0, 15.0, 90.0, "MISSION-001"
//                ),
//                createTelemetry(
//                        "DRONE-102", 20.2600, 70.4127,
//                        95.0, "IDLE",
//                        0.0, 0.0, 0.0, null
//                ),
//                createTelemetry(
//                        "DRONE-103", 27.2610, 78.4130,
//                        30.0, "RETURNING",
//                        80.0, 10.0, 270.0, "MISSION-003"
//                ),
//
//                createTelemetry(
//                        "DRONE-104", 28.2610, 80.4130,
//                        35.0, "FLYING",
//                        88.0, 15.0, 275.0, "MISSION-004"
//                )
//        );
//    }
//
//    private DroneTelemetry createTelemetry(
//            String droneId,
//            double latitude,
//            double longitude,
//            double battery,
//            String status,
//            double altitude,
//            double speed,
//            double heading,
//            String missionId) {
//
//        DroneTelemetry telemetry = new DroneTelemetry();
//
//        telemetry.setDroneId(droneId);
//        telemetry.setLatitude(latitude);
//        telemetry.setLongitude(longitude);
//        telemetry.setBattery(battery);
//        telemetry.setStatus(status);
//        telemetry.setAltitude(altitude);
//        telemetry.setSpeed(speed);
//        telemetry.setHeading(heading);
//        telemetry.setMissionId(missionId);
//        telemetry.setTimestamp(Instant.now());
//        return telemetry;
//    }
//
//    public Optional<DroneTelemetry> getTelemetryByDroneId(String droneId) {
//
//        return getMockTelemetry()
//                .stream()
//                .filter(drone ->
//                        drone.getDroneId().equalsIgnoreCase(droneId))
//                .findFirst();
//    }
//
//}

package com.aerosaga.backend.service;

import com.aerosaga.backend.dto.DroneTelemetry;
import com.aerosaga.backend.entity.Drone;
import com.aerosaga.backend.repository.DroneRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TelemetryService {

    private final DroneRepository droneRepository;

    public TelemetryService(DroneRepository droneRepository) {
        this.droneRepository = droneRepository;
    }

    public List<DroneTelemetry> getRegisteredDroneTelemetry() {

        return droneRepository.findAll()
                .stream()
                .map(this::createTelemetryFromDrone)
                .toList();
    }

    private DroneTelemetry createTelemetryFromDrone(Drone drone) {

        DroneTelemetry telemetry = new DroneTelemetry();

        telemetry.setDroneId(drone.getDroneId());
        telemetry.setLatitude(drone.getBaseLatitude());
        telemetry.setLongitude(drone.getBaseLongitude());

        // Demo values: actual sensor data is not available yet.
        telemetry.setBattery(100.0);
        telemetry.setStatus(drone.getStatus());
        telemetry.setAltitude(0.0);
        telemetry.setSpeed(0.0);
        telemetry.setHeading(0.0);
        telemetry.setMissionId(null);
        telemetry.setTimestamp(Instant.now());

        return telemetry;
    }

    public Optional<DroneTelemetry> getTelemetryByDroneId(
            String droneId) {

        return getRegisteredDroneTelemetry()
                .stream()
                .filter(drone ->
                        drone.getDroneId()
                                .equalsIgnoreCase(droneId))
                .findFirst();
    }
}
