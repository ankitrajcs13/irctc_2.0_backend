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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            // TODO : Optimize this getTravelDayForStation function->
            LocalDate calculatedTrainStartDate = travelDate.minusDays(getTravelDayForStation(sourceStation)-1);
            routes = routeRepository.findTrainsBetweenStationsOnDate(sourceStation, destinationStation, calculatedTrainStartDate);
        } else {
            // Fetch all routes between the stations
            routes = routeRepository.findTrainsBetweenStations(sourceStation, destinationStation);
        }

        // Ensure stations are loaded to avoid LazyInitializationException
        routes.forEach(route -> route.getStations().size());

        // Convert entities to DTOs
        return routes.stream()
                .map(route -> convertToRouteDTO(route, travelDate, sourceStation, destinationStation))
                .collect(Collectors.toList());
    }

    private int getTravelDayForStation(String stationName) {
        return routeRepository.findAll().stream()  // Fetch all routes (could optimize by filtering for relevant ones)
                .flatMap(route -> route.getStations().stream())  // Get all stations from all routes
                .filter(rs -> rs.getStation().getName().equals(stationName))  // Find the correct station
                .map(RouteStation::getDay)  // Extract the day
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Station not found in any route: " + stationName));
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

            Map<String, Map<String, Object>> availableSeatsWithFare = new HashMap<>();
            for (String bogieType : availableSeatsByBogieType.keySet()) {
                BigDecimal fare = calculateFare(route.getTrain().getTrainNumber(), sourceStation, destinationStation, bogieType, false);
                Map<String, Object> seatAndFareInfo = new HashMap<>();
                seatAndFareInfo.put("availableSeats", availableSeatsByBogieType.get(bogieType));
                seatAndFareInfo.put("fare", fare);
                availableSeatsWithFare.put(bogieType, seatAndFareInfo);
            }
            routeDTO.setAvailableSeatsWithFare(availableSeatsWithFare);
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

    private BigDecimal calculateFare(String trainNumber, String sourceStation, String destinationStation, String bogieType, boolean isTatkal) {
        // Fetch the route to get station details
        Route route = routeRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new IllegalArgumentException("No route found for train number: " + trainNumber));

        // Variables to store segment positions and total distance
        int startSegment = -1, endSegment = -1;
        double distance = 0.0;

        for (RouteStation rs : route.getStations()) {
            if (rs.getStation().getName().equalsIgnoreCase(sourceStation)) {
                startSegment = rs.getStationOrder();
            }
            if (rs.getStation().getName().equalsIgnoreCase(destinationStation)) {
                endSegment = rs.getStationOrder();
            }
        }

        if (startSegment == -1 || endSegment == -1 || startSegment >= endSegment) {
            throw new IllegalArgumentException("Invalid source or destination station provided.");
        }

        // Fetch the distance from the database
        int finalEndSegment = endSegment;
        distance = route.getStations().stream()
                .filter(rs -> rs.getStationOrder() == finalEndSegment)
                .findFirst()
                .map(rs -> rs.getDistance().replaceAll("[^0-9.]", "")) // Remove non-numeric characters
                .map(Double::parseDouble)
                .orElseThrow(() -> new IllegalArgumentException("Distance information not available"));


        // Base Fare per km for each class
        Map<String, Double> baseFarePerKm = Map.of(
                "SLEEPER", 0.40,
                "THIRD_AC", 1.00,
                "SECOND_AC", 1.50,
                "FIRST_AC", 2.50
        );
        bogieType = bogieType.trim().toUpperCase();
        // Get base fare rate for the class
        Double baseFareRate = baseFarePerKm.getOrDefault(bogieType, 0.0);
        BigDecimal baseFare = BigDecimal.valueOf(baseFareRate * distance);

        // Reservation Charges per class
        Map<String, Integer> reservationCharges = Map.of(
                "SLEEPER", 20,
                "THIRD_AC", 40,
                "SECOND_AC", 50,
                "FIRST_AC", 60
        );

        int reservationCharge = reservationCharges.getOrDefault(bogieType, 0);

        // Superfast Charge if distance > 200 km
        int superfastCharge = (distance > 200) ? 30 : 0;

        // GST (Only for AC classes)
        double gstRate = (bogieType.equalsIgnoreCase("THIRD_AC") ||
                bogieType.equalsIgnoreCase("SECOND_AC") ||
                bogieType.equalsIgnoreCase("FIRST_AC")) ? 0.05 : 0.0;
        BigDecimal gst = baseFare.multiply(BigDecimal.valueOf(gstRate));

        // Tatkal Markup (If applicable)
        BigDecimal tatkalMarkup = BigDecimal.ZERO;
        if (isTatkal) {
            Map<String, Integer> tatkalCharges = Map.of(
                    "SL", 100,
                    "3AC", 300,
                    "2A", 400,
                    "1A", 500
            );
            tatkalMarkup = BigDecimal.valueOf(tatkalCharges.getOrDefault(bogieType, 0));
        }

        return baseFare.add(BigDecimal.valueOf(reservationCharge))
                .add(BigDecimal.valueOf(superfastCharge))
                .add(gst)
                .add(tatkalMarkup)
                .setScale(0, RoundingMode.HALF_UP);
    }

}
