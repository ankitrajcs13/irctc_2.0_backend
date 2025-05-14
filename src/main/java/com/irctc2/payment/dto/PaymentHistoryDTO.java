package com.irctc2.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryDTO {

    private String paymentId;
    private Long userId;  // Instead of embedding the whole User object, use just the userId
    private Double amount;
    private String status;
    private String transactionDate;
}
