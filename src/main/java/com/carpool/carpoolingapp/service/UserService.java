package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.model.User;

import java.util.List;

public interface UserService {
    User registerUser(User user);
    List<User> getAllUsers();
    User findByEmail(String email);
}