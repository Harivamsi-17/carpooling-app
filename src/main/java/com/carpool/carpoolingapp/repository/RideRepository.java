package com.carpool.carpoolingapp.repository;

import com.carpool.carpoolingapp.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    @Query("SELECT r FROM Ride r " +
            "JOIN FETCH r.driver " +
            "LEFT JOIN FETCH r.bookings " +
            "WHERE r.driver.id = :driverId")
    List<Ride> findByDriverIdWithBookings(@Param("driverId") Long driverId);

    // REPLACE the old findByOrigin... method with this new @Query
    @Query("SELECT r FROM Ride r " +
            "JOIN FETCH r.driver " +
            "LEFT JOIN FETCH r.bookings " +
            "WHERE lower(r.origin) LIKE lower(concat('%', :origin, '%')) " +
            "AND lower(r.destination) LIKE lower(concat('%', :destination, '%'))")
    List<Ride> searchRidesWithDetails(@Param("origin") String origin, @Param("destination") String destination);

}