package com.irctc2.booking.service;

import com.irctc2.booking.entity.BookingStatus;
import com.irctc2.booking.model.Booking;
import com.irctc2.booking.repository.BookingRepository;
import com.irctc2.route.model.Route;
import com.irctc2.route.model.RouteStation;
import com.irctc2.route.repository.RouteRepository;
import com.irctc2.train.service.DiscordNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingServiceCron {

    private final BookingRepository bookingRepository;
    private final RouteRepository routeRepository;
    private final DiscordNotificationService discordNotificationService;

    public BookingServiceCron(BookingRepository bookingRepository, RouteRepository routeRepository, DiscordNotificationService discordNotificationService) {
        this.bookingRepository = bookingRepository;
        this.routeRepository = routeRepository;
        this.discordNotificationService = discordNotificationService;
    }

    /**
     *  This runs automatically every day at 2 AM IST
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Kolkata")
    public void scheduledExpiredBookings() {
        processExpiredBookings();
    }

    /**
     *  This can be called manually from an API
     */
    public void processExpiredBookings() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        discordNotificationService.sendDiscordMessage("Manually triggering expired bookings.");

        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);
        int expiredCount = 0;

        for (Booking booking : confirmedBookings) {
            LocalDate calculatedEndDate = calculateEndDate(booking);
            if (calculatedEndDate.isBefore(yesterday)) {
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);
                expiredCount++;
            }
        }

        System.out.println("Expired bookings updated: " + expiredCount);
    }

    /**
     * Calculate the actual end date of a journey dynamically.
     */
    private LocalDate calculateEndDate(Booking booking) {
        Route route = routeRepository.findById(booking.getRouteId()).orElse(null);

        if (route != null) {
            RouteStation sourceRouteStation = route.getStations().stream()
                    .filter(rs -> rs.getStation().getName().equals(booking.getSourceStation()))
                    .findFirst().orElse(null);

            RouteStation destinationRouteStation = route.getStations().stream()
                    .filter(rs -> rs.getStation().getName().equals(booking.getDestinationStation()))
                    .findFirst().orElse(null);

            if (sourceRouteStation != null && destinationRouteStation != null) {
                int sourceDay = sourceRouteStation.getDay();
                int destinationDay = destinationRouteStation.getDay();

                return booking.getTravelDate().plusDays(destinationDay - sourceDay);
            }
        }

        return booking.getTravelDate(); // Default: Assume same-day travel
    }
}
