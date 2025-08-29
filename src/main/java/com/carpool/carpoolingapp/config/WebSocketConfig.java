package com.carpool.carpoolingapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Sets up the message broker for topics the client will subscribe to
        registry.enableSimpleBroker("/topic", "/queue");
        // Sets the prefix for messages sent from the client to the server
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Exposes the WebSocket endpoint that the client will connect to
        // SockJS is a fallback for browsers that don't support WebSockets
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5500", "http://127.0.0.1:5500", "http://127.0.0.1:8888")
                .withSockJS();
    }
}