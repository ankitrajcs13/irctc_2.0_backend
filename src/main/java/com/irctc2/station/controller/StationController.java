package com.irctc2.station.controller;

import com.irctc2.station.model.Station;
import com.irctc2.station.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    @Autowired
    private StationService stationService;

    @PostMapping
    public Station createStation(@RequestBody Station station) {
        return stationService.createStation(station);
    }

    @GetMapping
    public List<Station> getAllStations() {
        return stationService.getAllStations();
    }

    // Update an existing station by ID
    @PutMapping("/{id}")
    public Station updateStation(@PathVariable Long id, @RequestBody Station updatedStation) {
        return stationService.updateStation(id, updatedStation);
    }

    // Delete a station by ID
    @DeleteMapping("/{id}")
    public void deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
    }
}
