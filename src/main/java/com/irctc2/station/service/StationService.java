package com.irctc2.station.service;


import com.irctc2.station.model.Station;
import com.irctc2.station.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    public Station createStation(Station station) {
        return stationRepository.save(station);
    }

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public Station getStationByName(String name) {
        return stationRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Station not found: " + name));
    }

    public Station updateStation(Long id, Station updatedStation) {
        Station existingStation = stationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Station with ID " + id + " does not exist."));

        existingStation.setName(updatedStation.getName()); // Only updating the name for now
        return stationRepository.save(existingStation);
    }

    public void deleteStation(Long id) {
        if (!stationRepository.existsById(id)) {
            throw new IllegalArgumentException("Station with ID " + id + " does not exist.");
        }
        stationRepository.deleteById(id);
    }
}
