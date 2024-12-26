package com.irctc2.train.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BogieDTO {
    private String type;    // Bogie type (e.g., Sleeper, AC, General)
    private String bogieName;
    private Integer seatCount; // Total number of seats in the bogie
    private Map<Integer, Boolean> seatAvailability;
}
