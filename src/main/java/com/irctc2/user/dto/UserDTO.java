package com.irctc2.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private Date dob;
    private String address;
    private String pincode;
    private String nationality;
    private String role;
    private String status;
    private String profileImageUrl;
    private Boolean isVerified;
    private Date passwordExpireDate;
    private Date createdAt;
    private Date updatedAt;

    public UserDTO(Long id, String username, String email, String firstName, String lastName, String phoneNumber,
                   String gender, Date dob, String address, String pincode, String nationality, String role,
                   String status, String profileImageUrl, Boolean isVerified, Date passwordExpireDate,
                   Date createdAt, Date updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.dob = dob;
        this.address = address;
        this.pincode = pincode;
        this.nationality = nationality;
        this.role = role;
        this.status = status;
        this.profileImageUrl = profileImageUrl;
        this.isVerified = isVerified;
        this.passwordExpireDate = passwordExpireDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
