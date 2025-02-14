package com.irctc2.train.service;

import com.irctc2.booking.model.Booking;
import com.irctc2.train.model.SeatAvailability;
import com.irctc2.train.model.Train;
import com.irctc2.train.repository.SeatAvailabilityRepository;
import com.irctc2.train.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeatBookingService {

    private final SeatAvailabilityRepository seatAvailabilityRepository;
    private final TrainRepository trainRepository;

    public SeatBookingService(SeatAvailabilityRepository seatAvailabilityRepository,
                              TrainRepository trainRepository) {
        this.seatAvailabilityRepository = seatAvailabilityRepository;
        this.trainRepository = trainRepository;
    }


    public List<SeatAvailability> allocateSeatsForSegment(String trainNumber, LocalDate travelDate,
                                                          String bogieType, int passengerCount,
                                                          int startSegment, int endSegment,
                                                          String bookingReference) {
        // 1. Fetch the train to obtain its id (and any other needed details).
        Train train = trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new IllegalArgumentException("Train not found for number: " + trainNumber));
        Long trainId = train.getId();

        // 2. Retrieve all SeatAvailability records for the train on the travel date.
        List<SeatAvailability> seats = seatAvailabilityRepository.findByTrainIdAndTravelDateAndBogieType(trainId, travelDate, bogieType);

        // 3. Iterate over seats, filtering by bogie type and checking segment availability.
        List<SeatAvailability> allocatedSeats = new ArrayList<>();
        for (SeatAvailability seat : seats) {
            // Check if the seat is available for the requested segment.
            if (checkAndBookSeatForSegment(seat, startSegment, endSegment)) {
                allocatedSeats.add(seat);
            }
            // If we have allocated enough seats, break out of the loop.
            if (allocatedSeats.size() == passengerCount) {
                break;
            }
        }

        // 4. If we haven't found enough seats, throw an exception.
        if (allocatedSeats.size() < passengerCount) {
            throw new RuntimeException("Not enough seats available in bogie type: " + bogieType +
                    " for the requested segment range.");
        }

        // 5. Save all the allocated seats in a single batch.
        seatAvailabilityRepository.saveAll(allocatedSeats);

        return allocatedSeats;
    }

    // Helper method to check and book a seat for the requested segment range
    public static boolean checkAndBookSeatForSegment(SeatAvailability seat, int startSegment, int endSegment) {
        // Check if the seat is available for the requested segment range
        if (seat.isSegmentRangeAvailable(startSegment, endSegment)) {
            // Mark the seat as booked for the requested segment range (update the bit mask)
            seat.bookSegments(startSegment, endSegment);
            return true; // Seat successfully booked
        }
        return false; // Seat not available
    }
}
