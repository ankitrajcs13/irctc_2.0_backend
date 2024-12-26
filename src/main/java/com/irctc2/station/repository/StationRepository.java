package com.irctc2.station.repository;

import com.irctc2.station.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    // Find station by its name (unique constraint assumed in the database)
    Optional<Station> findByName(String name);
}
