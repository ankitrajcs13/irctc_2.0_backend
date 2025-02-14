package com.irctc2.train.service;

import com.irctc2.train.dto.TrainDTO;
import com.irctc2.train.dto.BogieDTO;
import com.irctc2.train.model.Train;
import com.irctc2.train.model.Bogie;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrainMapper1 {

    // Convert Train entity to TrainDTO
    public static TrainDTO toDTO(Train train) {
        // Aggregate seat count by bogie type
        Map<String, Integer> seatCountByType = train.getBogies().stream()
                .collect(Collectors.groupingBy(
                        Bogie::getBogieType,
                        Collectors.summingInt(Bogie::getTotalSeats)
                ));

        // Convert aggregated seat counts to a list of BogieDTOs
        List<BogieDTO> bogieDTOs = seatCountByType.entrySet().stream()
                .map(entry -> BogieDTO.builder()
                        .type(entry.getKey())
                        .seatCount(entry.getValue()) // Total seats for this bogie type
                        .build()
                ).collect(Collectors.toList());

        // Build TrainDTO
        return TrainDTO.builder()
                .trainNumber(train.getTrainNumber())
                .name(train.getName())
                .source(train.getSource())
                .destination(train.getDestination())
                .departureTime(train.getDepartureTime())
                .arrivalTime(train.getArrivalTime())
                .runningDays(train.getRunningDays())
                .bogies(bogieDTOs) // Use the aggregated bogieDTOs
                .build();
    }
}
