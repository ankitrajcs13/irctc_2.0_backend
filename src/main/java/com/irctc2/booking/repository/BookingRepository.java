package com.irctc2.booking.repository;

import com.irctc2.booking.entity.BookingStatus;
import com.irctc2.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPnr(String pnr);
    List<Booking> findByUser_Email(String email);
    List<Booking> findByStatus(BookingStatus status);
}
