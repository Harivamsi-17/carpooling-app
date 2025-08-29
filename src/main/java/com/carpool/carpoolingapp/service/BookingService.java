package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.dto.BookingResponseDto;
import com.carpool.carpoolingapp.model.Booking;
import com.carpool.carpoolingapp.model.BookingStatus;

import java.util.List;

public interface BookingService {
    Booking createBooking(Long rideId);
    List<Booking> getBookingsForRide(Long rideId);
    Booking updateBookingStatus(Long bookingId, BookingStatus status);
    List<BookingResponseDto> getMyBookingsAsRider();
    Booking cancelBookingAsRider(Long bookingId);
    void deleteBooking(Long bookingId);
}