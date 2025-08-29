package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.dto.RideResponseDto;
import com.carpool.carpoolingapp.model.Ride;
import com.carpool.carpoolingapp.model.User;
import com.carpool.carpoolingapp.repository.RideRepository;
import com.carpool.carpoolingapp.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    public RideServiceImpl(RideRepository rideRepository, UserRepository userRepository) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Ride createRide(Ride ride) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User driver = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Driver not found."));
        ride.setDriver(driver);
        return rideRepository.save(ride);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> searchRides(String origin, String destination) {
        // CHANGE THIS LINE to call the new method
        List<Ride> rides = rideRepository.searchRidesWithDetails(origin, destination);

        // The rest of the method works as-is
        return rides.stream()
                .map(RideResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getMyRidesAsDriver() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User driver = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Driver not found."));

        // CHANGE THIS LINE to call the new method
        List<Ride> rides = rideRepository.findByDriverIdWithBookings(driver.getId());

        // The rest of the method works as-is
        return rides.stream()
                .map(RideResponseDto::from)
                .collect(Collectors.toList());
    }
}