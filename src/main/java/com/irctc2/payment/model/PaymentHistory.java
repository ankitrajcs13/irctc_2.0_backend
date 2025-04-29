package com.irctc2.payment.model;

import com.irctc2.booking.model.Booking;
import com.irctc2.user.model.User;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "payment_history")
@Data
public class PaymentHistory {

    @Id  // Marking paymentId as the primary key
    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "transaction_date")
    private String transactionDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true) // booking_id must be unique here
    private Booking booking;

}
