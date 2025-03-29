package com.irctc2.booking.controller;

import com.irctc2.booking.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Endpoint to create Razorpay order
    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@RequestParam int amount) {
        return paymentService.createOrder(amount);
    }

    // Endpoint to verify Razorpay payment
    @PostMapping("/verify-payment")
    public Map<String, String> verifyPayment(@RequestParam String paymentId) {
        return paymentService.verifyPayment(paymentId);
    }
}
