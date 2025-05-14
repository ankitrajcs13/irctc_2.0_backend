package com.irctc2.booking.mapper;


import com.irctc2.booking.dto.BookingDTO;
import com.irctc2.booking.dto.BookingResponseDTO;
import com.irctc2.booking.dto.PassengerDTO;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.model.Passenger;
import com.irctc2.payment.dto.PaymentHistoryDTO;
import com.irctc2.payment.model.PaymentHistory;
import com.irctc2.route.model.Route;
import com.irctc2.route.model.RouteStation;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.train.model.Train;
import com.irctc2.train.repository.TrainRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class BookingMapper {

    public static BookingResponseDTO toResponseDTO(Booking booking) {
        return new BookingResponseDTO(
                booking.getPnr(),
                booking.getTrainNumber(),
                booking.getTravelDate(),
                booking.getSourceStation(),
                booking.getDestinationStation(),
                booking.getBogieType(),
                booking.getTotalFare(),
                booking.getStatus().toString(),  // Ensure correct status format
                booking.getPassengers().stream()
                        .map(BookingMapper::convertToPassengerDTO)
                        .collect(Collectors.toList())
        );
    }

    public static BookingDTO toDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .pnr(booking.getPnr())
                .trainNumber(booking.getTrainNumber())
                .travelDate(booking.getTravelDate())
                .totalFare(booking.getTotalFare())
                .status(booking.getStatus())
                .bogieType(booking.getBogieType())
                .sourceStation(booking.getSourceStation())
                .destinationStation(booking.getDestinationStation())
                .sourceArrivalTime(null) // Placeholder, update if needed
                .sourceDepartureTime(null) // Placeholder, update if needed
                .destinationArrivalTime(null) // Placeholder, update if needed
                .destinationDepartureTime(null) // Placeholder, update if needed
                .passengers(booking.getPassengers().stream()
                        .map(p -> PassengerDTO.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .age(p.getAge())
                                .seatNumber(p.getSeatNumber())
                                .gender(p.getGender())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }


    public static BookingDTO convertToBookingDTO(Booking booking, RouteRepository routeRepository, TrainRepository trainRepository) {
        Route route = routeRepository.findById(booking.getRouteId()).orElse(null);

        String sourceArrivalTime = null;
        String sourceDepartureTime = null;
        int sourceDay = 0;
        String destinationArrivalTime = null;
        String destinationDepartureTime = null;
        int destinationDay = 0;

        String duration = null;

        if (route != null) {
            RouteStation sourceRouteStation = route.getStations().stream()
                    .filter(rs -> rs.getStation().getName().equals(booking.getSourceStation()))
                    .findFirst().orElse(null);

            RouteStation destinationRouteStation = route.getStations().stream()
                    .filter(rs -> rs.getStation().getName().equals(booking.getDestinationStation()))
                    .findFirst().orElse(null);

            if (sourceRouteStation != null) {
                sourceArrivalTime = sourceRouteStation.getArrivalTime();
                sourceDepartureTime = sourceRouteStation.getDepartureTime();
                sourceDay = sourceRouteStation.getDay();
            }

            if (destinationRouteStation != null) {
                destinationArrivalTime = destinationRouteStation.getArrivalTime();
                destinationDepartureTime = destinationRouteStation.getDepartureTime();
                destinationDay = destinationRouteStation.getDay();
            }
            if (sourceDepartureTime != null && destinationArrivalTime != null) {
                duration = calculateDuration(sourceDepartureTime,sourceDay, destinationArrivalTime, destinationDay);
            }
        }

        String trainName = trainRepository.findByTrainNumber(booking.getTrainNumber())
                .map(Train::getName) // Get train name if train is found
                .orElse("Unknown Train");

        return BookingDTO.builder()
                .id(booking.getId())
                .pnr(booking.getPnr())
                .trainNumber(booking.getTrainNumber())
                .trainName(trainName)
                .travelDate(booking.getTravelDate())
                .totalFare(booking.getTotalFare())
                .status(booking.getStatus())
                .bogieType(booking.getBogieType())
                .sourceStation(booking.getSourceStation())
                .destinationStation(booking.getDestinationStation())
                .sourceArrivalTime(sourceArrivalTime)
                .sourceDepartureTime(sourceDepartureTime)
                .destinationArrivalTime(destinationArrivalTime)
                .destinationDepartureTime(destinationDepartureTime)
                .travelDuration(duration)
                .passengers(booking.getPassengers().stream()
                        .map(BookingMapper::convertToPassengerDTO)
                        .collect(Collectors.toList()))
                .paymentHistory(convertToPaymentHistoryDTO(booking.getPaymentHistory()))
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

    public static PaymentHistoryDTO convertToPaymentHistoryDTO(PaymentHistory paymentHistory) {
        if (paymentHistory == null) {
            return null;
        }

        return PaymentHistoryDTO.builder()
                .paymentId(paymentHistory.getPaymentId())
                .userId(paymentHistory.getUser().getId())
                .amount(paymentHistory.getAmount())
                .status(paymentHistory.getStatus())
                .transactionDate(paymentHistory.getTransactionDate())
                .build();
    }

    private static String calculateDuration(String departureTime, int departureDay,
                                            String arrivalTime, int arrivalDay) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            LocalDate baseDate = LocalDate.now();

            LocalDateTime departureDateTime = LocalDateTime.of(
                    baseDate.plusDays(departureDay),
                    LocalTime.parse(departureTime, timeFormatter)
            );

            LocalDateTime arrivalDateTime = LocalDateTime.of(
                    baseDate.plusDays(arrivalDay),
                    LocalTime.parse(arrivalTime, timeFormatter)
            );

            Duration duration = Duration.between(departureDateTime, arrivalDateTime);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;

            return (minutes == 0) ? String.format("%dh", hours) : String.format("%02dh%02dmin", hours, minutes);
        } catch (Exception e) {
            return null; // Handle parsing errors
        }
    }

}
