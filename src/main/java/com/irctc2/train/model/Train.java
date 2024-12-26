package com.irctc2.train.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "trains")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trainNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String destination;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Bogie> bogies = new ArrayList<>();

    // Additional Fields
    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @ElementCollection
    @CollectionTable(name = "train_running_days", joinColumns = @JoinColumn(name = "train_id"))
    @Column(name = "day")
    private List<String> runningDays; // E.g., ["Monday", "Wednesday", "Friday"]

    // Convenience method to add a bogie
    public void addBogie(Bogie bogie) {
        bogies.add(bogie);
        bogie.setTrain(this);  // Ensure each bogie has a reference to this train
    }

    // Convenience method to remove a bogie
    public void removeBogie(Bogie bogie) {
        bogies.remove(bogie);
        bogie.setTrain(null); // Remove the reference in the Bogie entity
    }
}
