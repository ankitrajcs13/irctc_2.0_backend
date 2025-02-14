package com.irctc2.train.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AllocatedSeat {
    private Long bogieId;      // Unique ID of the bogie
    private String bogieName;  // Name of the bogie
    private Integer seatNumber; // Seat number within the bogie
}
