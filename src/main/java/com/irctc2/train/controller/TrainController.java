package com.irctc2.train.controller;

import com.irctc2.train.dto.TrainDTO;
import com.irctc2.train.model.CreateTrainRequest;
import com.irctc2.train.model.Train;
import com.irctc2.train.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    @Autowired
    private TrainService trainService;

    // API for fetching all trains
    @GetMapping("/fetch-all-trains")
    public ResponseEntity<List<TrainDTO>> getAllTrains() {
        List<TrainDTO> trains = trainService.getAllTrains();
        return ResponseEntity.ok(trains);
    }

    // Add a train
    @PostMapping
    public ResponseEntity<TrainDTO> addTrain(@RequestBody CreateTrainRequest request) {
        // Create Train object from the request data
        Train train = new Train();
        train.setTrainNumber(request.getTrainNumber());
        train.setName(request.getName());
        train.setSource(request.getSource());
        train.setDestination(request.getDestination());
        train.setDepartureTime(request.getDepartureTime());
        train.setArrivalTime(request.getArrivalTime());
        train.setRunningDays(request.getRunningDays());

        // Pass train along with bogie types and counts to the service
        TrainDTO createdTrain = trainService.addTrainWithBogieCounts(
                train, request.getBogieTypes(), request.getBogieCounts());

        // Return the created train as response
        return new ResponseEntity<>(createdTrain, HttpStatus.CREATED);
    }

    // Get train by train number
    @GetMapping("/{trainNumber}")
    public ResponseEntity<TrainDTO> getTrainByNumber(@PathVariable String trainNumber) {
        return trainService.findTrainByNumber(trainNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{trainNumber}")
    public ResponseEntity<TrainDTO> updateTrain(@PathVariable String trainNumber, @RequestBody Train updatedTrain) {
        TrainDTO updated = trainService.updateTrain(trainNumber, updatedTrain);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{trainNumber}")
    public ResponseEntity<Void> deleteTrain(@PathVariable String trainNumber) {
        trainService.deleteTrain(trainNumber);
        return ResponseEntity.noContent().build();
    }
}
