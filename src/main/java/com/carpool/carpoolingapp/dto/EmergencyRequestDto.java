package com.carpool.carpoolingapp.dto;

import lombok.Data;

@Data
public class EmergencyRequestDto {
    private Double requesterLat;
    private Double requesterLng;
    private String hospitalName;
    private Double hospitalLat;
    private Double hospitalLng;
}