package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.dto.EmergencyRequestDto;
import com.carpool.carpoolingapp.dto.EmergencyResponseDto;
import com.carpool.carpoolingapp.model.EmergencyRequest;
import com.carpool.carpoolingapp.model.User;
import com.carpool.carpoolingapp.model.UserRole;
import com.carpool.carpoolingapp.repository.EmergencyRequestRepository;
import com.carpool.carpoolingapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmergencyService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyService.class);
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GeocodingService geocodingService; // 1. ADD THIS FIELD

    // 2. UPDATE THE CONSTRUCTOR TO INCLUDE GeocodingService
    public EmergencyService(EmergencyRequestRepository emergencyRequestRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, GeocodingService geocodingService) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.geocodingService = geocodingService;
    }

    public EmergencyRequest createEmergencyRequest(EmergencyRequestDto requestDto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User requester = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found."));

        EmergencyRequest newRequest = new EmergencyRequest();
        newRequest.setRequester(requester);
        newRequest.setRequesterLat(requestDto.getRequesterLat());
        newRequest.setRequesterLng(requestDto.getRequesterLng());
        newRequest.setHospitalName(requestDto.getHospitalName());
        newRequest.setStatus("PENDING");

        EmergencyRequest savedRequest = emergencyRequestRepository.save(newRequest);

        Map<String, String> notificationPayload = Map.of(
                "type", "EMERGENCY_REQUEST",
                "message", "New emergency ride request from " + requester.getFullName() + " to " + savedRequest.getHospitalName(),
                "requestId", savedRequest.getId().toString()
        );

        logger.info("Broadcasting emergency request ID: {}", savedRequest.getId());
        messagingTemplate.convertAndSend("/topic/emergency-alerts", notificationPayload);

        return savedRequest;
    }

    @Transactional
    public EmergencyRequest acceptRequest(Long requestId) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User driver = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Driver not found."));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new IllegalStateException("Only drivers can accept requests.");
        }

        EmergencyRequest request = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Emergency request not found."));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("This request has already been accepted by another driver.");
        }

        request.setAssignedDriver(driver);
        request.setStatus("ACCEPTED");
        EmergencyRequest savedRequest = emergencyRequestRepository.save(request);

        User requester = savedRequest.getRequester();
        Map<String, String> notificationPayload = Map.of(
                "type", "EMERGENCY_ACCEPTED",
                "message", "Your emergency request has been accepted by driver " + driver.getFullName() + "!"
        );

        logger.info("Sending emergency accepted notification to user: {}", requester.getEmail());
        messagingTemplate.convertAndSendToUser(
                requester.getEmail(),
                "/queue/notifications",
                notificationPayload
        );

        return savedRequest;
    }

    @Transactional
    public void completeRequest(Long requestId) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User driver = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        EmergencyRequest request = emergencyRequestRepository.findById(requestId).orElseThrow();

        if (!request.getAssignedDriver().getId().equals(driver.getId())) {
            throw new IllegalStateException("You are not the assigned driver for this request.");
        }
        request.setStatus("COMPLETED");
        emergencyRequestRepository.save(request);
    }

    @Transactional
    public void cancelRequestByRider(Long requestId) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User rider = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        EmergencyRequest request = emergencyRequestRepository.findById(requestId).orElseThrow();

        if (!request.getRequester().getId().equals(rider.getId())) {
            throw new IllegalStateException("You are not the owner of this request.");
        }
        request.setStatus("CANCELLED");
        emergencyRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Optional<EmergencyResponseDto> getActiveRequestForDriver() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User driver = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return emergencyRequestRepository.findByAssignedDriverIdAndStatus(driver.getId(), "ACCEPTED")
                .map(request -> {
                    String address = geocodingService.getAddressFromCoordinates(request.getRequesterLat(), request.getRequesterLng());
                    return EmergencyResponseDto.from(request, address);
                });
    }

    @Transactional(readOnly = true)
    public Optional<EmergencyResponseDto> getActiveRequestForRider() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User rider = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return emergencyRequestRepository.findByRequesterIdAndStatusIn(rider.getId(), List.of("PENDING", "ACCEPTED"))
                .map(request -> {
                    String address = geocodingService.getAddressFromCoordinates(request.getRequesterLat(), request.getRequesterLng());
                    return EmergencyResponseDto.from(request, address);
                });
    }
}