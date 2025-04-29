package com.irctc2.payment.repository;

import com.irctc2.payment.model.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, String> {

    // Find all payment history for a specific user by userId
    List<PaymentHistory> findByUserId(Long userId);

    // Find payment history by paymentId (useful for querying by payment ID)
    PaymentHistory findByPaymentId(String paymentId);
}
