package com.irctc2.route.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.irctc2.route.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    // Custom JPQL Query
    @Query("SELECT DISTINCT r FROM Route r " +
            "JOIN r.stations s1 " +
            "JOIN r.stations s2 " +
            "WHERE s1.station.name = :sourceStation " +
            "AND s2.station.name = :destinationStation " +
            "AND s1.stationOrder < s2.stationOrder")
    List<Route> findTrainsBetweenStations(
            @Param("sourceStation") String sourceStation,
            @Param("destinationStation") String destinationStation
    );

    @Query("SELECT r FROM Route r JOIN r.stations s1 JOIN r.stations s2 " +
            "WHERE s1.station.name = :sourceStation AND s2.station.name = :destinationStation " +
            "AND EXISTS (SELECT sa FROM SeatAvailability sa " +
            "WHERE sa.trainId = r.train.id AND sa.travelDate = :travelDate)")
    List<Route> findTrainsBetweenStationsOnDate(@Param("sourceStation") String sourceStation,
                                                @Param("destinationStation") String destinationStation,
                                                @Param("travelDate") LocalDate travelDate);



    // Find a route by the associated train ID
    Optional<Route> findByTrainId(Long trainId);

    @Query("SELECT r FROM Route r JOIN r.train t WHERE t.trainNumber = :trainNumber")
    Optional<Route> findByTrainNumber(@Param("trainNumber") String trainNumber);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Route r JOIN r.train t " +
            "WHERE t.trainNumber = :trainNumber AND r.id = :routeId")
    boolean existsByTrainNumberAndRouteId(@Param("trainNumber") String trainNumber, @Param("routeId") Long routeId);
}
