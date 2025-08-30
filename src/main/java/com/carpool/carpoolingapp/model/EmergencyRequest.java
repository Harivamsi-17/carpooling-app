package com.carpool.carpoolingapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false)
    private Double requesterLat;

    @Column(nullable = false)
    private Double requesterLng;

    @Column(nullable = false)
    private String hospitalName;

    private String status = "PENDING"; // PENDING, ACCEPTED, COMPLETED

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User assignedDriver;
}