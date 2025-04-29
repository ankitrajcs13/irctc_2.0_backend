package com.irctc2.booking.controller;

import com.irctc2.booking.dto.BookingDTO;
import com.irctc2.booking.dto.BookingResponseDTO;
import com.irctc2.booking.dto.CreateBookingRequest;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.service.BookingService;
import com.irctc2.booking.service.BookingServiceCron;
import com.irctc2.payment.service.PaymentService;
import com.irctc2.security.jwt.JwtTokenProvider;
import com.irctc2.train.service.DiscordNotificationService;
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
    private BookingServiceCron bookingServiceCron;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DiscordNotificationService discordNotificationService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> createBooking(@RequestBody CreateBookingRequest request,
                                                @RequestParam boolean verifyPayment,
                                                @RequestParam String paymentId,
                                                            @RequestHeader("Authorization") String token) {

        // Extract email from token
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // TODO -> HANDLE HOW ADMIN CAN DO BOOKINGS USING ROLE FROM TOKEN
        String email = jwtTokenProvider.getUsernameFromToken(token);

        // Verify payment status before creating the booking
        String paymentStatus = "MANUAL";  // Default status when verifyPayment is false
        if(verifyPayment) {
            Map<String, String> paymentVerificationResponse = paymentService.verifyPayment(paymentId, email, true);

            if (!"success".equals(paymentVerificationResponse.get("status"))) {
                return ResponseEntity.badRequest().body(paymentVerificationResponse.get("message"));
            }
            paymentStatus = "SUCCESS";
        }

        Booking booking = bookingService.createBooking(request, email, paymentId, paymentStatus);

        // TODO -> Currently we are only sending 1 passenger to webhook
        String discordMessage = String.format(
                "**🎟️ Booking Confirmed!**\n" + "**PNR:** %s\n" + "**Train Number:** %s\n" + "**Travel Date:** %s\n" + "**Source:** %s\n" + "**Destination:** %s\n" +
                        "**Bogie Type:** %s\n" + "**Total Fare:** ₹%s\n" + "**Booking Status:** ✅ %s\n\n" + "**👥 Passenger Details:**\n" +
                        "\t🆔 ID: %d\n" + "\t👤 Name: %s\n" + "\t🎂 Age: %d\n" + "\t🚻 Gender: %s\n" + "\t💺 Seat Number: %s",
                booking.getPnr(),booking.getTrainNumber(),booking.getTravelDate(),booking.getSourceStation(),booking.getDestinationStation(),
                booking.getBogieType(), booking.getTotalFare(), booking.getBookingStatus(), booking.getPassengers().getFirst().getId(), booking.getPassengers().getFirst().getName(),
                booking.getPassengers().getFirst().getAge(), booking.getPassengers().getFirst().getGender(), booking.getPassengers().getFirst().getSeatNumber()
        );

        discordNotificationService.sendDiscordMessage(discordMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body("Booking Created Successfully");
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

    @PostMapping("/expire/manual")
    public String manuallyExpireBookings() {
        bookingServiceCron.processExpiredBookings();
        return "Expired bookings processed manually!";
    }
}
