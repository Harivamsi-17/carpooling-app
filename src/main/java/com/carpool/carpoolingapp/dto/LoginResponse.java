package com.carpool.carpoolingapp.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private final String token;
    private final String fullName;
}