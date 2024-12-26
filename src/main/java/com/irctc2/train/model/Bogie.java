package com.irctc2.train.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@Setter
@Table(name = "bogies")
public class Bogie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bogieType; // E.g., Sleeper, AC, General

    @Column(nullable = false)
    private String bogieName; // Unique name like S1, B1, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    // Instead of storing availableSeats as a list, we use a Map for efficiency
    @ElementCollection
    @CollectionTable(name = "bogie_seat_availability", joinColumns = @JoinColumn(name = "bogie_id"))
    @MapKeyColumn(name = "seat_number")
    @Column(name = "is_available")
    private Map<Integer, Boolean> seatAvailability = new HashMap<>(); // Map seat number to availability

    // Method to generate seats based on the bogie type
    public void generateSeats() {
        int seatCount;

        // Determine seat count based on bogie type
        switch (bogieType.substring(0, 2)) { // Use the first character of bogieName to determine type
            case "SL": // Sleeper
                seatCount = 72;
                break;
            case "TH": // Third AC
                seatCount = 72;
                break;
            case "SE": // Second AC
                seatCount = 54;
                break;
            case "FI": // First AC
                seatCount = 18;
                break;
            case "GE": // General
                seatCount = 90;
                break;
            default:
                throw new IllegalArgumentException("Invalid bogie name prefix: " + bogieType);
        }

        // Populate seat availability for each seat number
        for (int i = 1; i <= seatCount; i++) {
            seatAvailability.put(i, true); // All seats are available by default
        }
    }


    // Convenience methods for managing seat availability
    public void addSeatAvailability(Integer seatNumber, Boolean isAvailable) {
        seatAvailability.put(seatNumber, isAvailable);
    }

    public void removeSeatAvailability(Integer seatNumber) {
        seatAvailability.remove(seatNumber);
    }

    public Boolean getSeatAvailability(Integer seatNumber) {
        return seatAvailability.get(seatNumber);
    }

    public Integer getTotalSeats() {
        return seatAvailability.size();
    }
}
