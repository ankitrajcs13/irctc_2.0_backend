package com.irctc2.payment.service;

import com.irctc2.booking.model.Booking;
import com.irctc2.payment.model.PaymentHistory;
import com.irctc2.payment.repository.PaymentHistoryRepository;
import com.irctc2.user.model.User;
import com.irctc2.user.repository.UserRepository;
import com.razorpay.*;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PaymentService {

    @Value("${razorpay.keyId}")
    private String keyId;

    @Value("${razorpay.keySecret}")
    private String keySecret;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    // Save Payment History
    public void savePaymentHistory(String paymentId, User user, Double amount, String status, String transactionDate, Booking booking) {
        PaymentHistory paymentHistory = new PaymentHistory();

        paymentHistory.setPaymentId(paymentId);
        paymentHistory.setUser(user);
        paymentHistory.setAmount(amount);
        paymentHistory.setStatus(status);
        paymentHistory.setTransactionDate(transactionDate);
        paymentHistory.setBooking(booking);

        paymentHistoryRepository.save(paymentHistory);
    }


    // Get Payment History by User ID
    public List<PaymentHistory> getPaymentHistory(Long userId) {
        return paymentHistoryRepository.findByUserId(userId);
    }

    // Get Payment History by Payment ID
    public PaymentHistory getPaymentByPaymentId(String paymentId) {
        return paymentHistoryRepository.findByPaymentId(paymentId);
    }

    public String processRefund(String paymentId) {
        RazorpayClient razorpayClient;
        String refundStatus = "Refund Failed";  // Default message

        try {
            razorpayClient = new RazorpayClient(keyId, keySecret);

            // Fetch payment details
            Payment payment = razorpayClient.payments.fetch(paymentId);

            // Check if payment is eligible for a refund (only captured payments can be refunded)
            if (payment.get("status").equals("captured")) {
                // Create a refund request
                Long amountInPaise = payment.get("amount");
                String receiptNumber = "LocoBharat-" + UUID.randomUUID().toString().substring(0, 8);


                JSONObject refundRequest = new JSONObject();
                refundRequest.put("amount", amountInPaise);
                refundRequest.put("speed", "normal"); // or "optimum" for instant refund

                // Optional: Add a receipt or notes
                refundRequest.put("receipt", receiptNumber);
                JSONObject notes = new JSONObject();
                notes.put("comment", "Refund for payment " + paymentId);
                refundRequest.put("notes", notes);

                // Initiate the refund
                Refund refund = razorpayClient.payments.refund(paymentId, refundRequest);
                refundStatus = "Refund Successful. Refund ID: " + refund.get("id");

                updatePaymentHistory(paymentId, "refunded");

            } else {
                refundStatus = "Payment not eligible for refund. Payment status: " + payment.get("status");
            }

        } catch (RazorpayException e) {
            refundStatus = "Error processing refund: " + e.getMessage();
        }

        return refundStatus;
    }

    // Method to update payment history after refund
    private void updatePaymentHistory(String paymentId, String status) {
        // Fetch payment history from the database
        PaymentHistory paymentHistory = paymentHistoryRepository.findByPaymentId(paymentId);
        if (paymentHistory != null) {
            paymentHistory.setStatus(status);
            paymentHistoryRepository.save(paymentHistory);
        }
    }

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
    @Transactional
    public Map<String, String> verifyPayment(String paymentId, String email, boolean verifyPayment) {
        RazorpayClient razorpayClient;
        Map<String, String> response = new HashMap<>();

        try {
            razorpayClient = new RazorpayClient(keyId, keySecret);

            // Fetch the payment details
            Payment payment = razorpayClient.payments.fetch(paymentId);

            Long amountInPaise = (Long) payment.get("amount");
            Double amount = amountInPaise / 100.0;
            String status = payment.get("status");
            String transactionDate = payment.get("created_at").toString();

            Optional<User> optionalUser = userRepository.findByEmail(email);
            User user = null;
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
            }
            // Check the payment status
            if (verifyPayment) {
                if (payment.get("status").equals("captured")) {
                    response.put("status", "success");
                    response.put("paymentId", paymentId);
                } else {
                    response.put("status", "failure");
                    response.put("message", "Payment not captured");
                }
            } else {
                response.put("status", "success");
                response.put("message", "Payment verification skipped for testing");
            }

        } catch (RazorpayException e) {
            response.put("status", "failure");
            response.put("message", "Error verifying payment: " + e.getMessage());
        }

        return response;
    }
}
