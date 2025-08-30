package com.carpool.carpoolingapp.repository;

import com.carpool.carpoolingapp.model.EmergencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {
    // Spring Data JPA will automatically provide all the necessary CRUD methods
    // like save(), findById(), findAll(), etc.
    Optional<EmergencyRequest> findByAssignedDriverIdAndStatus(Long driverId, String status);

    // Finds an active request for a specific rider
    Optional<EmergencyRequest> findByRequesterIdAndStatusIn(Long requesterId, java.util.List<String> statuses);
}