package com.carpool.carpoolingapp.config;

import com.carpool.carpoolingapp.service.JwtService;
import com.carpool.carpoolingapp.service.UserDetailsServiceImpl;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtChannelInterceptor(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Check if the message is a CONNECT command
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Get the Authorization header
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                String userEmail = jwtService.extractUsername(jwt);

                if (userEmail != null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                        // If token is valid, create an Authentication object and set it for the session
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        accessor.setUser(authToken);
                    }
                }
            }
        }
        return message;
    }
}