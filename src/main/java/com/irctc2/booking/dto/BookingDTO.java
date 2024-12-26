package com.irctc2.booking.dto;

import com.irctc2.booking.entity.BookingStatus;
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
    private LocalDate travelDate;
    private BigDecimal totalFare;
    private BookingStatus status;
    private String bogieType;
    private List<PassengerDTO> passengers;

    // Getters, setters, and constructor
}
