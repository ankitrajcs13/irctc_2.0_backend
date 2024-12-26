package com.irctc2.route.service;

import com.irctc2.route.dto.RouteDTO;
import com.irctc2.route.dto.RouteStationDTO;
import com.irctc2.route.model.Route;
import com.irctc2.route.model.RouteStation;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.station.model.Station;
import com.irctc2.station.repository.StationRepository;
import com.irctc2.train.model.Train;
import com.irctc2.train.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteService {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TrainService trainService;

    @Autowired
    private StationRepository stationRepository;

    public Route createRoute(String trainId, Route route) {
        Train train = trainService.getTrainById(trainId);
        route.setTrain(train);
        // Ensure all stations are fetched from the database
        for (RouteStation routeStation : route.getStations()) {
            String stationName = routeStation.getStation().getName();

            // Fetch station by name from the database
            Station station = stationRepository.findByName(stationName)
                    .orElseThrow(() -> new RuntimeException("Station not found: " + stationName));

            routeStation.setStation(station); // Set the managed Station entity
            routeStation.setRoute(route); // Associate the RouteStation with the Route
        }

        // Save the route with associated RouteStations
        return routeRepository.save(route);
    }

    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    public List<RouteDTO> findTrainsBetweenStations(String sourceStation, String destinationStation) {
        // Validate source and destination stations
        if (!stationRepository.findByName(sourceStation).isPresent()) {
            throw new RuntimeException("Source station not found: " + sourceStation);
        }
        if (!stationRepository.findByName(destinationStation).isPresent()) {
            throw new RuntimeException("Destination station not found: " + destinationStation);
        }
        if (sourceStation.equals(destinationStation)) {
            throw new RuntimeException("Source and destination stations cannot be the same.");
        }

        // Fetch routes matching the criteria
        List<Route> routes = routeRepository.findTrainsBetweenStations(sourceStation, destinationStation);

        // Convert entities to DTOs
        return routes.stream().map(this::convertToRouteDTO).collect(Collectors.toList());
    }

    private RouteDTO convertToRouteDTO(Route route) {
        RouteDTO routeDTO = new RouteDTO();
        routeDTO.setTrainNumber(route.getTrain().getTrainNumber());
        routeDTO.setTrainName(route.getTrain().getName());

        List<RouteStationDTO> stationDTOs = route.getStations().stream()
                .map(this::convertToRouteStationDTO)
                .collect(Collectors.toList());
        routeDTO.setStations(stationDTOs);

        return routeDTO;
    }

    private RouteStationDTO convertToRouteStationDTO(RouteStation station) {
        RouteStationDTO stationDTO = new RouteStationDTO();
        stationDTO.setStationName(station.getStation().getName());
        stationDTO.setStationOrder(station.getStationOrder());
        stationDTO.setDay(station.getDay());
        stationDTO.setArrivalTime(station.getArrivalTime());
        stationDTO.setDepartureTime(station.getDepartureTime());
        stationDTO.setHaltTime(station.getHaltTime());
        stationDTO.setDistance(station.getDistance());
        return stationDTO;
    }
}
