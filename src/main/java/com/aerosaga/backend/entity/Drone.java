
package com.aerosaga.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Drone {

    @Id
    @Column(name = "drone_id", nullable = false, length = 50)
    private String droneId;

    @Column(name = "base_latitude", nullable = false)
    private double baseLatitude;

    @Column(name = "base_longitude", nullable = false)
    private double baseLongitude;

    @Column(nullable = false, length = 20)
    private String status = "IDLE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.status == null) {
            this.status = "IDLE";
        }
    }
}
