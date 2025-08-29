package com.carpool.carpoolingapp.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @KafkaListener(topics = "booking-notifications", groupId = "carpooling-group")
    public void handleBookingNotification(String message) {
        // This is where you would normally send a push notification, email, or SMS.
        // For now, we'll just print it to the console to prove it's working.
        System.out.println("-----------------------------------------");
        System.out.println("NOTIFICATION RECEIVED: " + message);
        System.out.println("-----------------------------------------");
    }
    // ...
    @KafkaListener(topics = "emergency-alerts", groupId = "carpooling-group")
    public void handleEmergencyAlert(String message) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("CODE RED - EMERGENCY ALERT RECEIVED: " + message);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
}