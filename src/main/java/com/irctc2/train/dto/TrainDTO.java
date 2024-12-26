package com.irctc2.train.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainDTO {
    private String trainNumber;  // Unique train number
    private String name;         // Train name
    private String source;       // Source station
    private String destination;  // Destination station
    private List<BogieDTO> bogies; // List of bogies in the train
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private List<String> runningDays;
}
