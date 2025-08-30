package com.carpool.carpoolingapp.dto;

import com.carpool.carpoolingapp.model.EmergencyRequest;
import lombok.Data;

@Data
public class EmergencyResponseDto {
    private Long id;
    private String status;
    private String destinationHospital;
    private String riderName;
    private String driverName;
    private String pickupAddress; // The new field for the readable address

    public static EmergencyResponseDto from(EmergencyRequest request, String address) {
        EmergencyResponseDto dto = new EmergencyResponseDto();
        dto.setId(request.getId());
        dto.setStatus(request.getStatus());
        dto.setDestinationHospital(request.getHospitalName());
        dto.setRiderName(request.getRequester().getFullName());
        if (request.getAssignedDriver() != null) {
            dto.setDriverName(request.getAssignedDriver().getFullName());
        }
        dto.setPickupAddress(address);
        return dto;
    }
}