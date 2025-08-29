package com.carpool.carpoolingapp.repository;

import com.carpool.carpoolingapp.model.Booking;
import com.carpool.carpoolingapp.model.BookingStatus;
import com.carpool.carpoolingapp.model.Ride;
import com.carpool.carpoolingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRideId(Long rideId);

    // Use the simple method name again. We are removing the complex @Query.
    List<Booking> findByRiderId(Long riderId);

    boolean existsByRideAndRiderAndStatusIn(Ride ride, User rider, List<BookingStatus> statuses);
}