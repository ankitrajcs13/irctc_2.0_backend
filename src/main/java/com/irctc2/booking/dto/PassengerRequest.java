package com.irctc2.booking.dto;

import com.irctc2.booking.entity.Gender;
import lombok.Data;

@Data
public class PassengerRequest {
    private String name;       // Name of the passenger
    private int age;           // Age of the passenger
    private Gender gender;     // Gender of the passenger (e.g., MALE/FEMALE)
}
