package com.carpool.carpoolingapp.dto;

import com.carpool.carpoolingapp.model.Ride;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RideDto {

    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private Integer availableSeats;
    private BigDecimal price;

    // A helper method to convert this DTO into a Ride entity
    public Ride toRideEntity() {
        Ride ride = new Ride();
        ride.setOrigin(this.origin);
        ride.setDestination(this.destination);
        ride.setDepartureTime(this.departureTime);
        ride.setAvailableSeats(this.availableSeats);
        ride.setPrice(this.price);
        return ride;
    }
}