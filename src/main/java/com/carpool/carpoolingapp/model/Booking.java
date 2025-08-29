package com.carpool.carpoolingapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride; // The specific trip this ticket is for

    @ManyToOne
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider; // The specific person this ticket belongs to

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

}