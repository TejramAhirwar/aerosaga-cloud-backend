package com.aerosaga.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DroneCommandRequest {
    private String command;
    private Double latitude;
    private Double longitude;
    private Double altitude;
}
