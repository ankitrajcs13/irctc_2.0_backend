package com.irctc2.train.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateTrainRequest {

    private String trainNumber;
    private String name;
    private String source;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private List<String> runningDays; // List of days on which the train runs
    private List<String> bogieTypes;  // List of bogie types (e.g., ["Sleeper", "Second AC", "General"])
    private List<Integer> bogieCounts; // List of bogie counts for each bogie type (e.g., [12, 6, 2, 4])
}
