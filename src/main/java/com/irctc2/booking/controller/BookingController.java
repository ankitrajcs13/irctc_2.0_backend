package com.irctc2.booking.controller;

import com.irctc2.booking.dto.BookingDTO;
import com.irctc2.booking.dto.CreateBookingRequest;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.service.BookingService;
import com.irctc2.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<BookingDTO> getBooking(@PathVariable String pnr) {
        BookingDTO booking = bookingService.getBookingByPnr(pnr); // Use a service method to fetch by PNR
        return ResponseEntity.ok(booking);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBookings() {
        Map<String, Object> response = bookingService.getAllBookings();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<List<BookingDTO>> getBookingsForAuthenticatedUser(
            @RequestHeader("Authorization") String token) {
        // Remove 'Bearer ' prefix from the token
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Extract email from token
        String email = jwtTokenProvider.getUsernameFromToken(token);

        // Fetch bookings for the authenticated user
        List<BookingDTO> bookings = bookingService.getBookingsForAuthenticatedUser(email);

        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{pnr}/cancel")
    public ResponseEntity<BookingDTO> cancelBooking(@PathVariable String pnr) {
        BookingDTO canceledBooking = bookingService.cancelBooking(pnr);
        return ResponseEntity.ok(canceledBooking);
    }
}
