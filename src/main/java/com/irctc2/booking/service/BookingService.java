package com.irctc2.booking.service;

import com.irctc2.booking.dto.*;
import com.irctc2.booking.mapper.BookingMapper;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.entity.BookingStatus;
import com.irctc2.booking.model.Passenger;
import com.irctc2.booking.repository.BookingRepository;
import com.irctc2.booking.repository.PassengerRepository;
import com.irctc2.route.model.Route;
import com.irctc2.route.model.RouteStation;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.train.dto.AllocatedSeat;
import com.irctc2.train.model.SeatAvailability;
import com.irctc2.train.repository.SeatAvailabilityRepository;
import com.irctc2.train.service.SeatBookingService;
import com.irctc2.train.service.TrainMapper;
import com.irctc2.train.service.TrainService;
import com.irctc2.user.model.User;
import com.irctc2.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
    private SeatAvailabilityRepository seatAvailabilityRepository;

    @Autowired
    private TrainService trainService; // Service to manage Train & Seat availability.

    @Autowired
    private SeatBookingService seatAvailabilityService; // Service to manage Train & Seat availability.

    @Transactional
    public BookingResponseDTO createBooking(CreateBookingRequest request, String email) {
        // Fetch the route for the given train number
        Route route = routeRepository.findByTrainNumber(request.getTrainNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No route found for train number: " + request.getTrainNumber()));

        // Use the fetched route's ID
        Long routeId = route.getId();
        // Validate train and route
        trainService.validateTrainAndRoute(request.getTrainNumber(), routeId);

        // Determine the segment range using station orders.
        // (Assuming your route stations include the source and destination names)
        int startSegment = -1;
        int endSegment = -1;
        // Iterate through the route's station list.
        for (RouteStation rs : route.getStations()) {
            // Compare station names (ignoring case).
            if (rs.getStation().getName().equalsIgnoreCase(request.getSourceStation())) {
                startSegment = rs.getStationOrder();
            }
            if (rs.getStation().getName().equalsIgnoreCase(request.getDestinationStation())) {
                endSegment = rs.getStationOrder();
            }
        }

        // Validate that both stations were found and that the source comes before the destination.
        if (startSegment == -1 || endSegment == -1 || startSegment >= endSegment) {
            throw new IllegalArgumentException("Invalid source or destination station provided.");
        }

        String bookingReference = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Check seat availability
        int totalPassengers = request.getPassengers().size();
        List<SeatAvailability> allocatedSeats = seatAvailabilityService.allocateSeatsForSegment(
                request.getTrainNumber(),      // train number
                request.getTravelDate(),       // travel date
                request.getBogieType(),        // bogie type
                request.getPassengers().size(),// number of passengers
                startSegment,                  // start segment index
                endSegment,                    // end segment index
                bookingReference               // booking reference for logging/tracking
        );

        // Calculate fare
        BigDecimal totalFare = calculateFare(
                totalPassengers,
                request.getTrainNumber(),
                request.getSourceStation(),
                request.getDestinationStation(),
                request.getBogieType(),
                false
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Create booking
        Booking booking = new Booking();
        booking.setPnr(bookingReference);
        booking.setTrainNumber(request.getTrainNumber());
        booking.setTravelDate(request.getTravelDate());
        booking.setRouteId(route.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBogieType(request.getBogieType());
        booking.setTotalFare(totalFare);
        booking.setUser(user);
        booking.setSourceStation(request.getSourceStation());
        booking.setDestinationStation(request.getDestinationStation());
        // (Set any additional booking properties as needed.)

        // 6. Create passenger records and assign allocated seats.
        List<Passenger> passengers = new ArrayList<>();
        List<PassengerRequest> passengerRequests = request.getPassengers();
        for (int i = 0; i < passengerRequests.size(); i++) {
            PassengerRequest pr = passengerRequests.get(i);
            // Get the allocated seat for this passenger.
            SeatAvailability seat = allocatedSeats.get(i);
            Passenger passenger = new Passenger();
            passenger.setName(pr.getName());
            passenger.setAge(pr.getAge());
            passenger.setGender(pr.getGender());
            // Create a formatted seat identifier, e.g., "BogieName-SeatNumber".
            passenger.setSeatNumber(seat.getBogieName() + "-" + seat.getSeatNumber());
            passenger.setBooking(booking);
            passengers.add(passenger);
        }
        booking.setPassengers(passengers);

        // 7. Persist the booking and passenger details.
        bookingRepository.save(booking);
        passengerRepository.saveAll(passengers);

        // Directly return BookingResponseDTO
        return BookingMapper.toResponseDTO(booking);
    }

    private BigDecimal calculateFare(int totalPassengers, String trainNumber, String sourceStation, String destinationStation, String bogieType, boolean isTatkal) {
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

        // Total fare per passenger
        BigDecimal perPassengerFare = baseFare.add(BigDecimal.valueOf(reservationCharge))
                .add(BigDecimal.valueOf(superfastCharge))
                .add(gst)
                .add(tatkalMarkup)
                .setScale(0, RoundingMode.HALF_UP);

        // Total fare for all passengers
        return perPassengerFare.multiply(BigDecimal.valueOf(totalPassengers));
    }


//    public BookingDTO getBooking(Long bookingId) {
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new RuntimeException("Booking not found"));
//
//        List<PassengerDTO> passengerDTOs = booking.getPassengers().stream()
//                .map(p -> new PassengerDTO(p.getId(), p.getName(), p.getAge(), p.getSeatNumber(), p.getGender()))
//                .collect(Collectors.toList());
//
//        return new BookingDTO(booking.getId(), booking.getPnr(),booking.getTrainNumber(),
//                booking.getTravelDate(),
//                booking.getTotalFare(),
//                booking.getStatus(), booking.getBogieType(),booking.getSourceStation(), booking.getDestinationStation(),
//                passengerDTOs);
//    }

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
                .map(booking -> BookingMapper.convertToBookingDTO(booking, routeRepository))
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
