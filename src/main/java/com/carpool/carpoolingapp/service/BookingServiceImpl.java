package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.dto.BookingResponseDto;
import com.carpool.carpoolingapp.model.*;
import com.carpool.carpoolingapp.repository.BookingRepository;
import com.carpool.carpoolingapp.repository.RideRepository;
import com.carpool.carpoolingapp.repository.UserRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationService notificationService;

    public BookingServiceImpl(BookingRepository bookingRepository, RideRepository rideRepository, UserRepository userRepository, KafkaTemplate<String, String> kafkaTemplate,NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Booking createBooking(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalStateException("Ride not found."));
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User rider = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Rider not found."));
        if (Objects.equals(ride.getDriver().getId(), rider.getId())) {
            throw new IllegalStateException("Driver cannot book their own ride.");
        }
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        if (bookingRepository.existsByRideAndRiderAndStatusIn(ride, rider, activeStatuses)) {
            throw new IllegalStateException("You already have an active booking for this ride.");
        }
        if (ride.getAvailableSeats() <= 0) {
            throw new IllegalStateException("No available seats on this ride.");
        }
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setRider(rider);
        booking.setStatus(BookingStatus.PENDING);
        Booking savedBooking = bookingRepository.save(booking);

        notificationService.notifyDriver(
                ride.getDriver().getId(),
                "📢 New booking request from " + rider.getFullName() + " for ride #" + rideId
        );

        // ✅ (Optional) Still send to Kafka for logging
        kafkaTemplate.send("booking-notifications",
                "New booking request with ID: " + savedBooking.getId() + " for ride ID: " + rideId);

        return savedBooking;

    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalStateException("Ride not found."));
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User driver = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Driver not found."));
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new IllegalStateException("You are not authorized to view bookings for this ride.");
        }
        return bookingRepository.findByRideId(rideId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookingsAsRider() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User rider = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Rider not found."));
        List<Booking> bookings = bookingRepository.findByRiderId(rider.getId());
        return bookings.stream().map(booking -> {
            Ride ride = booking.getRide();
            long confirmedBookingsCount = ride.getBookings().stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .count();
            BigDecimal pricePerRider;
            if (ride.getPrice() == null || ride.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                pricePerRider = BigDecimal.ZERO;
            } else if (confirmedBookingsCount > 0) {
                pricePerRider = ride.getPrice().divide(BigDecimal.valueOf(confirmedBookingsCount), 2, RoundingMode.HALF_UP);
            } else {
                pricePerRider = ride.getPrice();
            }
            return BookingResponseDto.from(booking, pricePerRider);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Booking updateBookingStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found."));
        Ride ride = booking.getRide();
        if (booking.getStatus() == BookingStatus.CONFIRMED && status == BookingStatus.CANCELLED) {
            ride.setAvailableSeats(ride.getAvailableSeats() + 1);
            rideRepository.save(ride);
        }
        if (booking.getStatus() == BookingStatus.PENDING && status == BookingStatus.CONFIRMED) {
            if (ride.getAvailableSeats() <= 0) {
                throw new IllegalStateException("No available seats to confirm this booking.");
            }
            ride.setAvailableSeats(ride.getAvailableSeats() - 1);
            rideRepository.save(ride);
        }
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking cancelBookingAsRider(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found."));
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User rider = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Rider not found."));
        if (!booking.getRider().getId().equals(rider.getId())) {
            throw new IllegalStateException("You are not authorized to cancel this booking.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("This booking has already been cancelled.");
        }
        Ride ride = booking.getRide();
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            ride.setAvailableSeats(ride.getAvailableSeats() + 1);
            rideRepository.save(ride);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Override
    public void deleteBooking(Long bookingId) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found."));
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found."));
        if (!booking.getRider().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this booking.");
        }
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new IllegalStateException("Only cancelled bookings can be removed.");
        }
        bookingRepository.delete(booking);
    }
}