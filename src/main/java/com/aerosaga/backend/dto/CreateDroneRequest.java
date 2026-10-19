package com.aerosaga.backend.dto;



public record CreateDroneRequest(
        String droneId,
        Double baseLatitude,
        Double baseLongitude
) {}