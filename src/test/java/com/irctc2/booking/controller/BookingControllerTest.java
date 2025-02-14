package com.irctc2.booking.service;

import com.irctc2.booking.dto.CreateBookingRequest;
import com.irctc2.booking.dto.PassengerRequest;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.model.Passenger;
import com.irctc2.booking.repository.BookingRepository;
import com.irctc2.booking.repository.PassengerRepository;
import com.irctc2.route.model.Route;
import com.irctc2.route.model.RouteStation;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.train.model.SeatAvailability;
import com.irctc2.train.repository.SeatAvailabilityRepository;
import com.irctc2.train.service.SeatBookingService;
import com.irctc2.user.model.User;
import com.irctc2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatBookingService seatAvailabilityService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PassengerRepository passengerRepository;

    private CreateBookingRequest request;
    private Route mockRoute;
    private User mockUser;
    private List<SeatAvailability> mockSeats;

    @BeforeEach
    void setUp() {
        request = new CreateBookingRequest();
        request.setTrainNumber("12345");
        request.setTravelDate(LocalDate.now().plusDays(5));
        request.setBogieType("AC");
        request.setSourceStation("A");
        request.setDestinationStation("F");
        request.setEmail("test@example.com");
        request.setPassengers(List.of(new PassengerRequest("John Doe", 30, "M")));

        mockRoute = new Route();
        RouteStation source = new RouteStation();
        source.setStationOrder(1);
        source.setStationName("A");

        RouteStation destination = new RouteStation();
        destination.setStationOrder(5);
        destination.setStationName("F");

        mockRoute.setStations(List.of(source, destination));

        mockUser = new User();
        mockUser.setEmail("test@example.com");

        SeatAvailability seat = new SeatAvailability();
        seat.setBogieName("B1");
        seat.setSeatNumber(16);
        seat.setBookedSegments(0L);

        mockSeats = List.of(seat);
    }

    @Test
    void testSuccessfulBooking() {
        when(routeRepository.findByTrainNumber("12345")).thenReturn(Optional.of(mockRoute));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(seatAvailabilityService.allocateSeatsForSegment(any(), any(), any(), anyInt(), anyInt(), anyInt(), any())).thenReturn(mockSeats);
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(request);

        assertNotNull(booking);
        assertEquals("12345", booking.getTrainNumber());
        assertEquals("test@example.com", booking.getUser().getEmail());
        assertEquals(1, booking.getPassengers().size());
        assertEquals("B1-16", booking.getPassengers().get(0).getSeatNumber());

        verify(bookingRepository, times(1)).save(any());
        verify(passengerRepository, times(1)).saveAll(any());
    }

    @Test
    void testBookingFailsWhenRouteNotFound() {
        when(routeRepository.findByTrainNumber("12345")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(request));
        assertEquals("No route found for train number: 12345", exception.getMessage());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testBookingFailsWhenInvalidStationsProvided() {
        Route invalidRoute = new Route();
        invalidRoute.setStations(List.of()); // No stations

        when(routeRepository.findByTrainNumber("12345")).thenReturn(Optional.of(invalidRoute));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(request));
        assertEquals("Invalid source or destination station provided.", exception.getMessage());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testBookingFailsWhenSeatsUnavailable() {
        when(routeRepository.findByTrainNumber("12345")).thenReturn(Optional.of(mockRoute));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(seatAvailabilityService.allocateSeatsForSegment(any(), any(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenThrow(new RuntimeException("No seats available"));

        Exception exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request));
        assertEquals("No seats available", exception.getMessage());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testBookingFailsWhenUserNotFound() {
        when(routeRepository.findByTrainNumber("12345")).thenReturn(Optional.of(mockRoute));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request));
        assertEquals("User not found", exception.getMessage());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testTransactionRollbackOnFailure() {
        when(routeRepository.findByTrainNumber("12345")).thenReturn(Optional.of(mockRoute));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(seatAvailabilityService.allocateSeatsForSegment(any(), any(), any(), anyInt(), anyInt(), anyInt(), any())).thenReturn(mockSeats);

        doThrow(new RuntimeException("Simulated failure after seat allocation")).when(bookingRepository).save(any());

        Exception exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request));
        assertEquals("Simulated failure after seat allocation", exception.getMessage());

        verify(passengerRepository, never()).saveAll(any()); // Ensures passengers were never saved
    }
}
