package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.dto.RideResponseDto;
import com.carpool.carpoolingapp.model.Ride;

import java.util.List;

public interface RideService {
    Ride createRide(Ride ride);
    List<RideResponseDto> searchRides(String origin, String destination);
    List<RideResponseDto> getMyRidesAsDriver();
}