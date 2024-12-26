package com.irctc2.user.dto;

import lombok.Data;

@Data
public class UpdateRequest {
    private String firstName;
    private String lastName;
    private String address;
    private Boolean isVerified;
    private String status; // E.g., Verified, Pending
    // Add any other fields you want to allow updates for
}
