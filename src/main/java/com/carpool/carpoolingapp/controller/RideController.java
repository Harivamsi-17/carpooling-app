package com.carpool.carpoolingapp.controller;

import com.carpool.carpoolingapp.dto.RideDto; // Or a similar DTO for creating rides
import com.carpool.carpoolingapp.dto.RideResponseDto;
import com.carpool.carpoolingapp.model.Ride;
import com.carpool.carpoolingapp.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    // Assumes you have a RideDto for creating a ride
    @PostMapping
    public ResponseEntity<Ride> createRide(@RequestBody RideDto rideDto) {
        Ride ride = rideDto.toRideEntity(); // A method to convert DTO to entity
        Ride createdRide = rideService.createRide(ride);
        return ResponseEntity.ok(createdRide);
    }

    @GetMapping("/search")
    public ResponseEntity<List<RideResponseDto>> searchRides(@RequestParam String origin, @RequestParam String destination) {
        return ResponseEntity.ok(rideService.searchRides(origin, destination));
    }

    @GetMapping("/my-rides")
    public ResponseEntity<List<RideResponseDto>> getMyRides() {
        return ResponseEntity.ok(rideService.getMyRidesAsDriver());
    }
}