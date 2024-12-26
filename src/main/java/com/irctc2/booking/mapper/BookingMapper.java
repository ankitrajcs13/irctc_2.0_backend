package com.irctc2.booking.mapper;


import com.irctc2.booking.dto.BookingDTO;
import com.irctc2.booking.dto.PassengerDTO;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.model.Passenger;

import java.util.stream.Collectors;

public class BookingMapper {

    public static BookingDTO toDTO(Booking booking) {
        return new BookingDTO(
                booking.getId(),
                booking.getPnr(),
                booking.getTrainNumber(),
                booking.getTravelDate(),
                booking.getTotalFare(),
                booking.getStatus(),
                booking.getBogieType(),
                booking.getPassengers().stream()
                        .map(p -> new PassengerDTO(
                                p.getId(),
                                p.getName(),
                                p.getAge(),
                                p.getSeatNumber(),
                                p.getGender()
                        ))
                        .collect(Collectors.toList())
        );
    }

    public static BookingDTO convertToBookingDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .pnr(booking.getPnr())
                .trainNumber(booking.getTrainNumber())
                .travelDate(booking.getTravelDate())
                .totalFare(booking.getTotalFare())
                .status(booking.getStatus())
                .passengers(booking.getPassengers().stream()
                        .map(BookingMapper::convertToPassengerDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    private static PassengerDTO convertToPassengerDTO(Passenger passenger) {
        return PassengerDTO.builder()
                .id(passenger.getId())
                .name(passenger.getName())
                .age(passenger.getAge())
                .gender(passenger.getGender())
                .seatNumber(passenger.getSeatNumber())
                .build();
    }
}
