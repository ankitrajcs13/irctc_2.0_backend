package com.irctc2.route.model;

import com.irctc2.station.model.Station;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@Table(name = "route_stations")
public class RouteStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many RouteStations belong to a single Route
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    // Each RouteStation is associated with a single Station
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private int stationOrder; // Order of the station in the route (e.g., 0, 1, 2)

    @Column(nullable = false)
    private int day; // Day the train reaches this station (e.g., Day 1, Day 2)

    @Column(nullable = false)
    private String arrivalTime; // Changed from LocalTime to String

    @Column(nullable = false)
    private String departureTime; // Changed from LocalTime to String

    @Column(nullable = true)
    private String haltTime; // Optional, added for textual halt times

    @Column(nullable = true)
    private String distance; // Optional, added for distance
}
