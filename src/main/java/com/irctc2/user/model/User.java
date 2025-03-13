package com.irctc2.user.model;

import com.irctc2.booking.model.Booking;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    // Getters and Setters
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "gender", length = 1)
    private String gender;

    @Column(name = "dob")
    @Temporal(TemporalType.DATE)
    private Date dob;

    @Column(name = "address")
    private String address;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "role", length = 20)
    private String role = "PASSENGER"; // Default role

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";  // Default status

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "is_verified")
    private Boolean isVerified = false; // Default to not verified

    @Column(name = "password_expire_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordExpireDate;

    @Column(name = "update_ip")
    private String updateIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();
}
