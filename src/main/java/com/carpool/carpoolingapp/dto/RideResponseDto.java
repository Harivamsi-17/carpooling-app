package com.carpool.carpoolingapp.dto;

import com.carpool.carpoolingapp.model.BookingStatus;
import com.carpool.carpoolingapp.model.Ride;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
public class RideResponseDto {
    private Long id;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private Integer availableSeats;
    private String driverFullName;
    private BigDecimal totalPrice; // The total price set by the driver
    private BigDecimal currentPricePerRider; // The dynamically calculated price

    // Static factory method to build the DTO from a Ride entity
    public static RideResponseDto from(Ride ride) {
        RideResponseDto dto = new RideResponseDto();
        dto.setId(ride.getId());
        dto.setOrigin(ride.getOrigin());
        dto.setDestination(ride.getDestination());
        dto.setDepartureTime(ride.getDepartureTime());
        dto.setAvailableSeats(ride.getAvailableSeats());
        dto.setDriverFullName(ride.getDriver().getFullName());
        dto.setTotalPrice(ride.getPrice());

        // Perform the dynamic price calculation
        long confirmedBookingsCount = 0;
        if (ride.getBookings() != null) {
            confirmedBookingsCount = ride.getBookings().stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .count();
        }

        BigDecimal pricePerRider;
        if (ride.getPrice() == null || ride.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            pricePerRider = BigDecimal.ZERO;
        } else {
            // THIS IS THE FIX: We add 1 to the count for the new rider who is searching
            pricePerRider = ride.getPrice().divide(BigDecimal.valueOf(confirmedBookingsCount + 1), 2, RoundingMode.HALF_UP);
        }
        dto.setCurrentPricePerRider(pricePerRider);

        return dto;
    }
}