package com.irctc2.booking.model;

import com.irctc2.booking.entity.BookingStatus;
import com.irctc2.user.model.User;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pnr;

    @Column(nullable = false)
    private String trainNumber;

    @Column(nullable = false)
    private Long routeId;

    @Column(nullable = false)
    private LocalDate travelDate;

    @Column(nullable = false)
    private BigDecimal totalFare;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(nullable = false)
    private String bogieType; // New field to store bogie type

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Passenger> passengers = new ArrayList<>();

    // Many-to-One relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

