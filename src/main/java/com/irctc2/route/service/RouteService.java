package com.irctc2.route.service;

import com.irctc2.route.dto.RouteDTO;
import com.irctc2.route.dto.RouteStationDTO;
import com.irctc2.route.model.Route;
import com.irctc2.route.model.RouteStation;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.station.model.Station;
import com.irctc2.station.repository.StationRepository;
import com.irctc2.train.model.SeatAvailability;
import com.irctc2.train.model.Train;
import com.irctc2.train.repository.BogieRepository;
import com.irctc2.train.repository.SeatAvailabilityRepository;
import com.irctc2.train.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RouteService {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TrainService trainService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private SeatAvailabilityRepository seatAvailabilityRepository;

    @Autowired
    private BogieRepository bogieRepository;

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

    // TODO FIX THIS - USE SOME DIFF DTO OR USE IF
//    public List<RouteDTO> getAllRoutes() {
//        List<Route> routes = routeRepository.findAll();
//
//        // Convert each Route entity to RouteDTO
//        // Convert entities to DTOs using a lambda expression
//        return routes.stream()
//                .map(route -> convertToRouteDTO(route, travelDate, startSegment, endSegment, bogieType))
//                .collect(Collectors.toList());
//
//    }

    public List<RouteDTO> findTrainsBetweenStations(String sourceStation, String destinationStation, LocalDate travelDate) {
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

        List<Route> routes;
        if (travelDate != null) {
            // Filter routes by travelDate
            routes = routeRepository.findTrainsBetweenStationsOnDate(sourceStation, destinationStation, travelDate);
        } else {
            // Fetch all routes between the stations
            routes = routeRepository.findTrainsBetweenStations(sourceStation, destinationStation);
        }

            // Convert entities to DTOs
        // Convert entities to DTOs using a lambda expression
        return routes.stream()
                .map(route -> convertToRouteDTO(route, travelDate, sourceStation, destinationStation))
                .collect(Collectors.toList());

    }

    private RouteDTO convertToRouteDTO(Route route, LocalDate travelDate, String sourceStation, String destinationStation) {
        RouteDTO routeDTO = new RouteDTO();
        routeDTO.setTrainNumber(route.getTrain().getTrainNumber());
        routeDTO.setTrainName(route.getTrain().getName());
        routeDTO.setArrivalTime(route.getTrain().getArrivalTime());
        routeDTO.setDepartureTime(route.getTrain().getDepartureTime());
        routeDTO.setTravelTime(calculateDuration(route.getTrain().getDepartureTime(), route.getTrain().getArrivalTime()));
        routeDTO.setSourceStation(sourceStation);
        routeDTO.setDestinationStation(destinationStation);

        // ROUTE LIST (NOT NEEDED CURRENTLY)
//        List<RouteStationDTO> stationDTOs = route.getStations().stream()
//                .map(this::convertToRouteStationDTO)
//                .collect(Collectors.toList());
//        routeDTO.setStations(stationDTOs);

        List<RouteStation> routeStations = route.getStations();
        int startSegment = -1;
        int endSegment = -1;
        for (int i = 0; i < routeStations.size(); i++) {
            RouteStation station = routeStations.get(i);
            if (station.getStation().getName().equals(sourceStation)) {
                startSegment = station.getStationOrder();
            }
            if (station.getStation().getName().equals(destinationStation)) {
                endSegment = station.getStationOrder();
            }
        }

        // Get available seats if a travelDate is provided
        if (travelDate != null) {
            Map<String, Integer>  availableSeatsByBogieType = getAvailableSeats(route.getTrain().getId(), travelDate, startSegment, endSegment);
            routeDTO.setTravelDate(travelDate);
            routeDTO.setAvailableSeats(availableSeatsByBogieType);
        }

        return routeDTO;
    }

    private Map<String, Integer> getAvailableSeats(Long trainId, LocalDate travelDate, int startSegment, int endSegment) {
        List<String> distinctBogieTypes = bogieRepository.findDistinctBogieTypesByTrainId(trainId);

        Map<String, Integer> availableSeatsByBogieType = new HashMap<>();

        // Iterate over each bogieType and calculate available seats for that bogieType
        for (String bogieType : distinctBogieTypes) {
            int availableSeats = 0;

            // Fetch seat availability based on trainId, travelDate, and bogieType
            List<SeatAvailability> seatAvailabilities = seatAvailabilityRepository.findByTrainIdAndTravelDateAndBogieType(trainId, travelDate, bogieType);

            // Iterate over the seat availabilities and check if the seat is available for the dynamic segment range (startSegment to endSegment)
            for (SeatAvailability seatAvailability : seatAvailabilities) {
                if (seatAvailability.isSegmentRangeAvailable(startSegment, endSegment)) {
                    availableSeats++;
                }
            }

            // Store available seats for this bogieType in the map
            availableSeatsByBogieType.put(bogieType, availableSeats);
        }

        return availableSeatsByBogieType;
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

    private String calculateDuration(LocalDateTime departureTime, LocalDateTime arrivalTime) {
        if (departureTime == null || arrivalTime == null) {
            return "N/A"; // Handle cases where times are missing
        }

        Duration duration = Duration.between(departureTime, arrivalTime);
        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        // Build formatted string like "52 hours 30 minutes"
        StringBuilder durationStr = new StringBuilder();
        if (hours > 0) {
            durationStr.append(hours).append(" h ");
        }
        if (minutes > 0) {
            durationStr.append(minutes).append(" min");
        }

        return durationStr.toString().trim();
    }


}
