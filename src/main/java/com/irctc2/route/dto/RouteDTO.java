package com.irctc2.route.dto;

import com.irctc2.route.dto.RouteStationDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RouteDTO {
    private String trainNumber;
    private String trainName;
    private String sourceStation;
    private String destinationStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String travelTime;
    private List<RouteStationDTO> stations;
    private Map<String, Map<String, Object>> availableSeatsWithFare;
    private LocalDate travelDate;
}
