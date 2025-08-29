package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.model.Ride;
import com.carpool.carpoolingapp.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumerService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final RideRepository rideRepository;

    public NotificationConsumerService(SimpMessagingTemplate messagingTemplate, RideRepository rideRepository) {
        this.messagingTemplate = messagingTemplate;
        this.rideRepository = rideRepository;
    }

    @KafkaListener(topics = "booking-notifications", groupId = "carpooling-group")
    public void listenForBookingNotifications(String message) {
        logger.info("Received Kafka message: {}", message);

        // Extract the ride ID from the message
        Long rideId = parseRideIdFromMessage(message);
        if (rideId == null) {
            logger.error("Could not parse rideId from message: {}", message);
            return;
        }

        // Find the ride to identify the driver
        rideRepository.findById(rideId).ifPresent(ride -> {
            String driverEmail = ride.getDriver().getEmail();

            // Create a structured notification payload
            Map<String, String> notificationPayload = Map.of(
                    "title", "New Booking Request!",
                    "message", "A rider has requested to book your ride from " + ride.getOrigin() + " to " + ride.getDestination() + "."
            );

            // Send a message to the specific driver's private queue
            logger.info("Sending WebSocket notification to user: {}", driverEmail);
            messagingTemplate.convertAndSendToUser(
                    driverEmail,          // The user (principal name) to send to
                    "/queue/notifications", // The private destination
                    notificationPayload     // The data to send
            );
        });
    }

    private Long parseRideIdFromMessage(String message) {
        // A simple parser to find the ride ID from a message like
        // "New booking request with ID: 4 for ride ID: 2"
        Pattern pattern = Pattern.compile("ride ID: (\\d+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }
}