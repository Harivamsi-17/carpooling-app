package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.dto.EmergencyRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmergencyDispatchService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EmergencyDispatchService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handleEmergencyRequest(EmergencyRequest request) {
        // Step 1: In a real app, you would use an API (like Google Maps or GraphHopper)
        // to find the nearest hospital to the request.currentUserLocation.
        String nearestHospital = "City General Hospital"; // We'll hardcode this for now.

        // Step 2: The dispatcher broadcasts the Code Red alert over the emergency channel.
        String alertMessage = "EMERGENCY ALERT: Ride needed from "
                + request.getCurrentUserLocation()
                + " to " + nearestHospital + ". Please respond if you are nearby and available.";

        kafkaTemplate.send("emergency-alerts", alertMessage);
    }
}