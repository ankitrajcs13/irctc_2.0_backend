package com.irctc2.booking.dto;


import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateBookingRequest {
    private String trainNumber;      // Train number for the booking
    private String routeId;           // ID of the route
    private LocalDate travelDate;   // Travel date
    private String bogieType;
    private String sourceStation;
    private String destinationStation;
    private List<PassengerRequest> passengers; // List of passengers in the booking
}
