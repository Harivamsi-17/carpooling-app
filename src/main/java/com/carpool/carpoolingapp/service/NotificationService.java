package com.carpool.carpoolingapp.service;


import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ✅ Called from BookingServiceImpl
    public void notifyDriver(Long driverId, String message) {
        // Each driver listens on /topic/driver/{driverId}
        messagingTemplate.convertAndSend("/topic/driver/" + driverId, message);
    }

    public void notifyRider(Long riderId, String message) {
        messagingTemplate.convertAndSend("/topic/rider/" + riderId, message);
    }
}