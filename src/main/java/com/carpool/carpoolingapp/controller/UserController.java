package com.carpool.carpoolingapp.controller;

import com.carpool.carpoolingapp.dto.LoginRequest;
import com.carpool.carpoolingapp.dto.LoginResponse;
import com.carpool.carpoolingapp.model.User;
import com.carpool.carpoolingapp.repository.UserRepository; // <-- Import this
import com.carpool.carpoolingapp.service.JwtService;
import com.carpool.carpoolingapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository; // <-- Add this

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository) { // <-- Add to constructor
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository; // <-- Initialize this
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        // 1. Authenticate the user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Find the user in the database to get their full name
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication."));

        // 3. Generate the JWT
        String token = jwtService.generateToken(user);

        // 4. Return the token AND the full name
        return new LoginResponse(token, user.getFullName());
    }
}