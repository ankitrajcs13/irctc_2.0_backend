package com.irctc2.train.service;

import com.irctc2.booking.model.Booking;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.route.service.RouteService;
import com.irctc2.train.dto.TrainDTO;
import com.irctc2.train.entity.BogieType;
import com.irctc2.train.model.Bogie;
import com.irctc2.train.model.Seat;
import com.irctc2.train.model.Train;
import com.irctc2.train.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainService {

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private RouteRepository routeRepository;

    public List<TrainDTO> getAllTrains() {
        List<Train> trains = trainRepository.findAll();
        return trains.stream()
                .map(TrainMapper::toDTO) // Convert each Train entity to TrainDTO
                .collect(Collectors.toList());
    }

    public Train addTrain(Train train) {
        for (Bogie bogie : train.getBogies()) {
            bogie.setTrain(train);  // Set the train reference on the bogie

            // Ensure each seat in the bogie has the bogie reference

        }

        return trainRepository.save(train);
    }

    public TrainDTO addTrainWithBogieCounts(Train train, List<String> bogieTypes, List<Integer> bogieCounts) {
        Map<String, Integer> bogieTypeCounters = new HashMap<>(); // Counter for each bogie type

        for (int i = 0; i < bogieTypes.size(); i++) {
            String bogieTypeString = bogieTypes.get(i);
            BogieType bogieType = BogieType.fromString(bogieTypeString); // Convert to enum
            int bogieCount = bogieCounts.get(i);

            // Initialize the counter for this bogie type if not already present
            bogieTypeCounters.putIfAbsent(bogieTypeString, 0);

            for (int j = 0; j < bogieCount; j++) {
                Bogie bogie = new Bogie();
                bogie.setBogieType(bogieTypeString); // Original string value
                bogie.setBogieName(bogieType.getPrefix() + (bogieTypeCounters.get(bogieTypeString) + 1)); // Name like S1, B1
                bogieTypeCounters.put(bogieTypeString, bogieTypeCounters.get(bogieTypeString) + 1); // Increment counter

                train.addBogie(bogie); // Add bogie to train

                // Generate seats for this bogie based on its type
                bogie.generateSeats();
            }
        }

        Train savedTrain = trainRepository.save(train);
        return TrainMapper.toDTO(savedTrain); // Return the DTO
    }

    // Update Train
    public TrainDTO updateTrain(String trainNumber, Train updatedTrain) {
        // Fetch the existing train by train number
        Train existingTrain = trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new IllegalArgumentException("Train with number " + trainNumber + " does not exist."));

        // Update basic train details
        existingTrain.setName(updatedTrain.getName());
        existingTrain.setSource(updatedTrain.getSource());
        existingTrain.setDestination(updatedTrain.getDestination());
        existingTrain.setDepartureTime(updatedTrain.getDepartureTime());
        existingTrain.setArrivalTime(updatedTrain.getArrivalTime());
        existingTrain.setRunningDays(updatedTrain.getRunningDays());

        // Update bogies
        List<Bogie> updatedBogies = updatedTrain.getBogies();
        if (updatedBogies != null) {
            // Clear existing bogies (orphan removal is enabled)
            existingTrain.getBogies().clear();

            // Add updated bogies to the train
            for (Bogie bogie : updatedBogies) {
                existingTrain.addBogie(bogie);
            }
        }

        // Save and return the updated train
        Train updated = trainRepository.save(existingTrain);
        return TrainMapper.toDTO(updated);
    }


    //Delete Train
    public void deleteTrain(String trainNumber) {
        // Fetch the train by train number
        Train train = trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new IllegalArgumentException("Train with number " + trainNumber + " does not exist."));

        // Delete the train, cascading the deletion to bogies
        trainRepository.delete(train);
    }

    public Optional<TrainDTO> findTrainByNumber(String trainNumber) {
        return trainRepository.findByTrainNumber(trainNumber)
                .map(TrainMapper::toDTO); // Convert Train entity to TrainDTO
    }

    public Train getTrainById(String trainNumber) {
        return trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new RuntimeException("Train not found with number: " + trainNumber));
    }

    public void validateTrainAndRoute(String trainNumber, Long routeId) {
        // Validate train existence
        Optional<Train> trainExists = trainRepository.findByTrainNumber(trainNumber);
        if (trainExists.isEmpty()) {
            throw new IllegalArgumentException("Train with number " + trainNumber + " does not exist.");
        }

        // Validate route existence for the train
        boolean routeExists = routeRepository.existsByTrainNumberAndRouteId(trainNumber, routeId);
        if (!routeExists) {
            throw new IllegalArgumentException("Route with ID " + routeId + " does not exist for train " + trainNumber + ".");
        }
    }

    public List<String> allocateSeats(String trainNumber, LocalDate travelDate, String bogieType, int passengerCount) {
        // Fetch the train by train number
        Train train = trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new IllegalArgumentException("Train with number " + trainNumber + " does not exist."));

        List<String> allocatedSeats = new ArrayList<>();

        // Filter bogies by the specified type
        List<Bogie> matchingBogies = train.getBogies().stream()
                .filter(bogie -> bogie.getBogieType().equalsIgnoreCase(bogieType))
                .collect(Collectors.toList());

        if (matchingBogies.isEmpty()) {
            throw new IllegalArgumentException("No bogies of type " + bogieType + " found in train " + trainNumber);
        }

        // Iterate through matching bogies to allocate seats
        for (Bogie bogie : matchingBogies) {
            for (Map.Entry<Integer, Boolean> entry : bogie.getSeatAvailability().entrySet()) {
                if (entry.getValue() && allocatedSeats.size() < passengerCount) {
                    String seatIdentifier = bogie.getBogieName() + "-" + entry.getKey(); // Format as <BogieName>-<SeatNumber>
                    allocatedSeats.add(seatIdentifier); // Allocate seat
                    bogie.addSeatAvailability(entry.getKey(), false); // Mark seat as unavailable
                }

                if (allocatedSeats.size() == passengerCount) {
                    break; // Stop if all passengers have seats
                }
            }

            if (allocatedSeats.size() == passengerCount) {
                break; // Stop if all passengers have seats
            }
        }

        if (allocatedSeats.size() < passengerCount) {
            throw new RuntimeException("Not enough seats available in bogie type: " + bogieType);
        }
        return allocatedSeats;
    }

//    private void deallocateSeats(Booking booking) {
//        // Fetch the train associated with the booking
//        Train train = trainRepository.findByTrainNumber(booking.getTrainNumber())
//                .orElseThrow(() -> new IllegalArgumentException("Train with number " + booking.getTrainNumber() + " does not exist."));
//
//        // Get the list of allocated seats and bogie type from the booking
////        List<Integer> allocatedSeats = booking.getAllocatedSeats(); // Example: List of seat numbers
//        String bogieType = booking.getBogieType(); // Example: "AC", "Sleeper", etc.
//
//        // Find the matching bogies in the train
//        List<Bogie> matchingBogies = train.getBogies().stream()
//                .filter(bogie -> bogie.getBogieType().equalsIgnoreCase(bogieType))
//                .collect(Collectors.toList());
//
//        if (matchingBogies.isEmpty()) {
//            throw new IllegalArgumentException("No bogies of type " + bogieType + " found in train " + train.getTrainNumber());
//        }
//
//        // Iterate through allocated seats and mark them as available
//        for (Bogie bogie : matchingBogies) {
//            for (Integer seat : allocatedSeats) {
//                if (bogie.getSeatAvailability().containsKey(seat)) {
//                    bogie.addSeatAvailability(seat, true); // Mark seat as available
//                }
//            }
//        }
//
//        // Save updated train details
//        trainRepository.save(train);
//    }


}
