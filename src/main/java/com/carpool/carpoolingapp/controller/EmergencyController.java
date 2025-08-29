package com.carpool.carpoolingapp.controller;

import com.carpool.carpoolingapp.dto.EmergencyRequest;
import com.carpool.carpoolingapp.service.EmergencyDispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    private final EmergencyDispatchService emergencyDispatchService;

    public EmergencyController(EmergencyDispatchService emergencyDispatchService) {
        this.emergencyDispatchService = emergencyDispatchService;
    }

    @PostMapping("/request")
    public ResponseEntity<String> requestEmergencyRide(@RequestBody EmergencyRequest request) {
        emergencyDispatchService.handleEmergencyRequest(request);
        return ResponseEntity.ok("Emergency alert has been broadcast to nearby drivers.");
    }
}