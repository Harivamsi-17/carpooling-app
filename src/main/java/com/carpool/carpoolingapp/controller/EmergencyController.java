package com.carpool.carpoolingapp.controller;

import com.carpool.carpoolingapp.dto.EmergencyRequestDto;
import com.carpool.carpoolingapp.dto.EmergencyResponseDto; // ADD THIS IMPORT
import com.carpool.carpoolingapp.model.EmergencyRequest;
import com.carpool.carpoolingapp.service.EmergencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @PostMapping("/request")
    public ResponseEntity<EmergencyRequest> requestEmergencyRide(@RequestBody EmergencyRequestDto requestDto) {
        EmergencyRequest newRequest = emergencyService.createEmergencyRequest(requestDto);
        return ResponseEntity.ok(newRequest);
    }

    @PostMapping("/request/{requestId}/accept")
    public ResponseEntity<EmergencyRequest> acceptEmergencyRequest(@PathVariable Long requestId) {
        EmergencyRequest acceptedRequest = emergencyService.acceptRequest(requestId);
        return ResponseEntity.ok(acceptedRequest);
    }

    @PostMapping("/request/{requestId}/complete")
    public ResponseEntity<Void> completeEmergencyRequest(@PathVariable Long requestId) {
        emergencyService.completeRequest(requestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/request/{requestId}/cancel")
    public ResponseEntity<Void> cancelEmergencyRequest(@PathVariable Long requestId) {
        emergencyService.cancelRequestByRider(requestId);
        return ResponseEntity.ok().build();
    }

    // CHANGE THE RETURN TYPE HERE
    @GetMapping("/driver/active")
    public ResponseEntity<EmergencyResponseDto> getActiveRequestForDriver() {
        return emergencyService.getActiveRequestForDriver()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // AND CHANGE THE RETURN TYPE HERE
    @GetMapping("/rider/active")
    public ResponseEntity<EmergencyResponseDto> getActiveRequestForRider() {
        return emergencyService.getActiveRequestForRider()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}