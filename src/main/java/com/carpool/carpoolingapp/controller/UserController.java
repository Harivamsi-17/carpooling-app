package com.carpool.carpoolingapp.controller;

import com.carpool.carpoolingapp.dto.LoginRequest;
import com.carpool.carpoolingapp.dto.LoginResponse;
import com.carpool.carpoolingapp.model.User;
import com.carpool.carpoolingapp.service.JwtService;
import com.carpool.carpoolingapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // The UserDetailsService is not needed here, so it can be removed from the constructor
    public UserController(UserService userService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User registeredUser = userService.registerUser(user);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        // Authenticate the user. If credentials are bad, this throws an exception.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        // If authentication is successful, find the user to generate a JWT.
        final User user = userService.findByEmail(loginRequest.getEmail());

        // --- THIS IS THE CORRECTED LINE ---
        // We now pass only the 'user' object, which is what the method requires.
        final String token = jwtService.generateToken(user);

        // Return the token and the user's full name.
        return ResponseEntity.ok(new LoginResponse(token, user.getFullName()));
    }
}