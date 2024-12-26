package com.irctc2.booking.service;

import com.irctc2.booking.dto.BookingDTO;
import com.irctc2.booking.dto.CreateBookingRequest;
import com.irctc2.booking.dto.PassengerDTO;
import com.irctc2.booking.mapper.BookingMapper;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.entity.BookingStatus;
import com.irctc2.booking.model.Passenger;
import com.irctc2.booking.repository.BookingRepository;
import com.irctc2.booking.repository.PassengerRepository;
import com.irctc2.route.model.Route;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.train.service.TrainMapper;
import com.irctc2.train.service.TrainService;
import com.irctc2.user.model.User;
import com.irctc2.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private TrainService trainService; // Service to manage Train & Seat availability.

    public Booking createBooking(CreateBookingRequest request) {
        // Fetch the route for the given train number
        Route route = routeRepository.findByTrainNumber(request.getTrainNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No route found for train number: " + request.getTrainNumber()));

        // Use the fetched route's ID
        Long routeId = route.getId();
        // Validate train and route
        trainService.validateTrainAndRoute(request.getTrainNumber(), routeId);

        // Check seat availability
        int totalPassengers = request.getPassengers().size();
        List<String> allocatedSeats = trainService.allocateSeats(request.getTrainNumber(), request.getTravelDate(),request.getBogieType(), totalPassengers);

        if (allocatedSeats.isEmpty() || allocatedSeats.size() < totalPassengers) {
            throw new RuntimeException("Not enough seats available.");
        }

        // Calculate fare
        BigDecimal totalFare = calculateFare(totalPassengers, request.getRouteId());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Create booking
        Booking booking = new Booking();
        booking.setPnr(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setTrainNumber(request.getTrainNumber());
        booking.setRouteId(routeId);
        booking.setTravelDate(request.getTravelDate());
        booking.setTotalFare(totalFare);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(user);
        booking.setBogieType(request.getBogieType());
        bookingRepository.save(booking);

        // Save passengers
        List<Passenger> passengers = request.getPassengers().stream()
                .map(p -> {
                    Passenger passenger = new Passenger();
                    passenger.setName(p.getName());
                    passenger.setAge(p.getAge());
                    passenger.setGender(p.getGender());
                    passenger.setSeatNumber(allocatedSeats.remove(0));
                    passenger.setBooking(booking);
                    return passenger;
                })
                .collect(Collectors.toList());
        passengerRepository.saveAll(passengers);

        return booking;
    }

    private BigDecimal calculateFare(int totalPassengers, String routeId) {
        // Implement fare calculation logic based on distance and class
        BigDecimal perPassengerFare = BigDecimal.valueOf(500); // Placeholder logic
        return perPassengerFare.multiply(BigDecimal.valueOf(totalPassengers));
    }

    public BookingDTO getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        List<PassengerDTO> passengerDTOs = booking.getPassengers().stream()
                .map(p -> new PassengerDTO(p.getId(), p.getName(), p.getAge(), p.getSeatNumber(), p.getGender()))
                .collect(Collectors.toList());

        return new BookingDTO(booking.getId(), booking.getPnr(),booking.getTrainNumber(),
                booking.getTravelDate(),
                booking.getTotalFare(),
                booking.getStatus(), booking.getBogieType(), passengerDTOs);
    }

    public BookingDTO getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new RuntimeException("Booking with PNR " + pnr + " not found."));

        // Convert Booking entity to DTO using the mapper
        return BookingMapper.toDTO(booking);
    }

    public Map<String, Object> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        List<BookingDTO> bookingDTOs = bookings.stream()
                .map(BookingMapper::toDTO) // Convert each Booking entity to BookingDTO
                .collect(Collectors.toList());

        // Create the response map
        Map<String, Object> response = new HashMap<>();
        response.put("count", bookingDTOs.size());
        response.put("bookings", bookingDTOs);

        return response;
    }

    public List<BookingDTO> getBookingsForAuthenticatedUser(String email) {
        // Fetch all bookings associated with the given email
        List<Booking> bookings = bookingRepository.findByUser_Email(email);

        // Convert to DTOs for response
        return bookings.stream()
                .map(BookingMapper::convertToBookingDTO)
                .collect(Collectors.toList());
    }


    public BookingDTO cancelBooking(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Booking with PNR " + pnr + " not found."));

        // Change booking status to "CANCELLED"
        booking.setStatus(BookingStatus.CANCELLED);

        // Optionally, handle seat reallocation/refund logic
        // Example: refundService.processRefund(booking);

        bookingRepository.save(booking);
        return BookingMapper.toDTO(booking); // Convert to DTO for response
    }
}
