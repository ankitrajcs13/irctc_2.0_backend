package com.irctc2.payment.controller;

import com.irctc2.booking.dto.CreateBookingRequest;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.repository.BookingRepository;
import com.irctc2.payment.model.PaymentHistory;
import com.irctc2.payment.service.PaymentService;
import com.irctc2.security.jwt.JwtTokenProvider;
import com.irctc2.user.model.User;
import com.irctc2.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // Endpoint to save payment history
    @PostMapping("/save-payment-history")
    public String savePaymentHistory(@RequestHeader("Authorization") String token,
                                     @RequestParam String paymentId,
                                     @RequestParam Double amount,
                                     @RequestParam String status,
                                     @RequestParam String transactionDate,
                                     @RequestParam Long bookingId) {

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String email = jwtTokenProvider.getUsernameFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking with ID " + bookingId + " not found."));

        paymentService.savePaymentHistory(paymentId, user, amount, status, transactionDate, booking);

        return "Payment history saved successfully.";
    }


    // Endpoint to get payment history by userId
    @GetMapping("/history")
    public List<PaymentHistory> getPaymentHistory(@RequestParam Long userId) {
        return paymentService.getPaymentHistory(userId);
    }

    // Endpoint to get payment details by paymentId
    @GetMapping("/payment")
    public PaymentHistory getPaymentByPaymentId(@RequestParam String paymentId) {
        return paymentService.getPaymentByPaymentId(paymentId);
    }

    // Endpoint to create Razorpay order
    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@RequestParam int amount) {
        return paymentService.createOrder(amount);
    }

    // Endpoint to verify Razorpay payment
//    @PostMapping("/verify-payment")
//    public Map<String, String> verifyPayment(@RequestParam String paymentId, @RequestHeader("Authorization") String token
//    , @RequestParam boolean verifyPayment, @RequestBody CreateBookingRequest bookingRequest) {
//        // Extract email from token
//        if (token.startsWith("Bearer ")) {
//            token = token.substring(7);
//        }
//
//        String email = jwtTokenProvider.getUsernameFromToken(token);
//        return paymentService.verifyPayment(paymentId, email, verifyPayment, bookingRequest);
//    }
}
