package com.irctc2.booking.dto;

import com.irctc2.booking.entity.BookingStatus;
import com.irctc2.payment.dto.PaymentHistoryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {
    private Long id;
    private String pnr;
    private String trainNumber;
    private String trainName;
    private LocalDate travelDate;
    private BigDecimal totalFare;
    private BookingStatus status;
    private String bogieType;
    private String sourceStation;
    private String sourceArrivalTime;
    private String sourceDepartureTime;
    private String destinationArrivalTime;
    private String destinationDepartureTime;
    private String destinationStation;
    private String travelDuration;
    private List<PassengerDTO> passengers;
    private PaymentHistoryDTO paymentHistory;

    // Getters, setters, and constructor
}
