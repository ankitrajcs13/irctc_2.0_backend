package com.irctc2.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class BookingResponseDTO {
    private String pnr;
    private String trainNumber;
    private LocalDate travelDate;
    private String sourceStation;
    private String destinationStation;
    private String bogieType;
    private BigDecimal totalFare;
    private String bookingStatus;
    private List<PassengerDTO> passengers;

}
