package com.carpool.carpoolingapp.dto;

import com.carpool.carpoolingapp.model.Booking;
import com.carpool.carpoolingapp.model.BookingStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long bookingId;
    private BookingStatus status;
    private Long rideId;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private String driverName;
    private BigDecimal currentPricePerRider;
    private Integer availableSeats; // ADD THIS LINE

    public static BookingResponseDto from(Booking booking, BigDecimal pricePerRider) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingId(booking.getId());
        dto.setStatus(booking.getStatus());
        dto.setRideId(booking.getRide().getId());
        dto.setOrigin(booking.getRide().getOrigin());
        dto.setDestination(booking.getRide().getDestination());
        dto.setDepartureTime(booking.getRide().getDepartureTime());
        dto.setDriverName(booking.getRide().getDriver().getFullName());
        dto.setCurrentPricePerRider(pricePerRider);
        dto.setAvailableSeats(booking.getRide().getAvailableSeats()); // AND ADD THIS LINE
        return dto;
    }
}