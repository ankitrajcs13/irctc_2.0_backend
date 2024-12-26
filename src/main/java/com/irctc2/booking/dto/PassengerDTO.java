package com.irctc2.booking.dto;


import com.irctc2.booking.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerDTO {
    private Long id;
    private String name;
    private int age;
    private String seatNumber;
    private Gender gender;

    // Getters, setters, and constructor
}
