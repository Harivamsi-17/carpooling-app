package com.carpool.carpoolingapp.controller;

import com.carpool.carpoolingapp.dto.BookingResponseDto;
import com.carpool.carpoolingapp.model.Booking;
import com.carpool.carpoolingapp.model.BookingStatus;
import com.carpool.carpoolingapp.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/rides/{rideId}/book")
    public ResponseEntity<Booking> createBooking(@PathVariable Long rideId) {
        Booking newBooking = bookingService.createBooking(rideId);
        return ResponseEntity.ok(newBooking);
    }

    @GetMapping("/bookings/ride/{rideId}")
    public ResponseEntity<List<Booking>> getBookingsForRide(@PathVariable Long rideId) {
        List<Booking> bookings = bookingService.getBookingsForRide(rideId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/my-bookings")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings() {
        List<BookingResponseDto> bookings = bookingService.getMyBookingsAsRider();
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<Booking> confirmBooking(@PathVariable Long bookingId) {
        Booking updatedBooking = bookingService.updateBookingStatus(bookingId, BookingStatus.CONFIRMED);
        return ResponseEntity.ok(updatedBooking);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long bookingId) {
        Booking updatedBooking = bookingService.updateBookingStatus(bookingId, BookingStatus.CANCELLED);
        return ResponseEntity.ok(updatedBooking);
    }

    @PostMapping("/bookings/{bookingId}/cancel-by-rider")
    public ResponseEntity<Booking> cancelBookingAsRider(@PathVariable Long bookingId) {
        Booking updatedBooking = bookingService.cancelBookingAsRider(bookingId);
        return ResponseEntity.ok(updatedBooking);
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long bookingId) {
        bookingService.deleteBooking(bookingId);
        return ResponseEntity.noContent().build();
    }
}