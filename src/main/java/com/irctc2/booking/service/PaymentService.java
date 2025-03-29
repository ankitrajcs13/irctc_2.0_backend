package com.irctc2.booking.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${razorpay.keyId}")
    private String keyId;

    @Value("${razorpay.keySecret}")
    private String keySecret;

    // Method to create Razorpay Order
    public Map<String, Object> createOrder(int amount) {
        RazorpayClient razorpayClient;
        Map<String, Object> response = new HashMap<>();

        try {
            razorpayClient = new RazorpayClient(keyId, keySecret);

            // Create order data
            Map<String, Object> orderRequestData = new HashMap<>();
            orderRequestData.put("amount", amount); // Amount in paise
            orderRequestData.put("currency", "INR");
            orderRequestData.put("receipt", "order_receipt_123");
            orderRequestData.put("payment_capture", 1); // Auto-capture payment

            // Convert Map to JSONObject
            JSONObject orderRequestJson = new JSONObject(orderRequestData);

            // Create the order
            Order order = razorpayClient.orders.create(orderRequestJson);

            // Prepare the response with order details
            response.put("id", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));

        } catch (RazorpayException e) {
            response.put("error", "Failed to create Razorpay order: " + e.getMessage());
        }

        return response;
    }

    // Method to verify Razorpay Payment
    public Map<String, String> verifyPayment(String paymentId) {
        RazorpayClient razorpayClient;
        Map<String, String> response = new HashMap<>();

        try {
            razorpayClient = new RazorpayClient(keyId, keySecret);

            // Fetch the payment details
            Payment payment = razorpayClient.payments.fetch(paymentId);

            // Check the payment status
            if (payment.get("status").equals("captured")) {
                response.put("status", "success");
                response.put("paymentId", paymentId);
            } else {
                response.put("status", "failure");
                response.put("message", "Payment not captured");
            }

        } catch (RazorpayException e) {
            response.put("status", "failure");
            response.put("message", "Error verifying payment: " + e.getMessage());
        }

        return response;
    }
}
