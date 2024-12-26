package com.irctc2.train.service;

import com.irctc2.train.dto.TrainDTO;
import com.irctc2.train.dto.BogieDTO;
import com.irctc2.train.model.Train;
import com.irctc2.train.model.Bogie;

import java.util.stream.Collectors;

public class TrainMapper {

    // Convert Train entity to TrainDTO
    public static TrainDTO toDTO(Train train) {
        return TrainDTO.builder()
                .trainNumber(train.getTrainNumber())
                .name(train.getName())
                .source(train.getSource())
                .destination(train.getDestination())
                .departureTime(train.getDepartureTime())
                .arrivalTime(train.getArrivalTime())
                .runningDays(train.getRunningDays())
                .bogies(train.getBogies().stream()
                        .map(TrainMapper::toBogieDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    // Convert Bogie entity to BogieDTO
    private static BogieDTO toBogieDTO(Bogie bogie) {
        return BogieDTO.builder()
                .type(bogie.getBogieType())
                .bogieName(bogie.getBogieName())
                .seatCount(bogie.getTotalSeats())
                .seatAvailability(bogie.getSeatAvailability())
                .build();
    }
}
