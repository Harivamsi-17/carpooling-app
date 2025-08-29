package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.exception.PhoneNumberAlreadyExistsException;
import com.carpool.carpoolingapp.model.User;
import com.carpool.carpoolingapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(User user) {
        // Check for existing email
        userRepository.findByEmail(user.getEmail()).ifPresent(s -> {
            throw new IllegalStateException("Email address is already registered.");
        });

        // ADD THIS CHECK for existing phone number
        userRepository.findByPhoneNumber(user.getPhoneNumber()).ifPresent(s -> {
            throw new PhoneNumberAlreadyExistsException("Phone number is already registered.");
        });

        // ... rest of the method (password encoding, save)
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}