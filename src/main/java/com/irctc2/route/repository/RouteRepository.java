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

    // TODO : THIS ONE IS USING ONE METHDO TO CALCULATE DATE on Java side by Pulling all routes first.
//    @Query("SELECT r FROM Route r JOIN r.stations s1 JOIN r.stations s2 " +
//            "WHERE s1.station.name = :sourceStation AND s2.station.name = :destinationStation " +
//            "AND EXISTS (SELECT sa FROM SeatAvailability sa " +
//            "WHERE sa.trainId = r.train.id AND sa.travelDate = :calculatedTrainStartDate)")
//    List<Route> findTrainsBetweenStationsOnDate(@Param("sourceStation") String sourceStation,
//                                                @Param("destinationStation") String destinationStation,
//                                                @Param("calculatedTrainStartDate") LocalDate calculatedTrainStartDate);

    // TODO : USING THIS AS OPTIMIZED VERSION -- Doing ALL Calculation on DB
    @Query(value = """
    SELECT DISTINCT r.*
    FROM routes r
    JOIN route_stations rs_source ON rs_source.route_id = r.id
    JOIN stations s_source ON rs_source.station_id = s_source.id
    JOIN route_stations rs_dest ON rs_dest.route_id = r.id
    JOIN stations s_dest ON rs_dest.station_id = s_dest.id
    WHERE s_source.name = :sourceStation
      AND s_dest.name = :destinationStation
      AND EXISTS (
        SELECT 1
        FROM seat_availability sa
        WHERE sa.train_id = r.train_id
          AND sa.travel_date = CAST((CAST(:travelDate AS timestamp) - make_interval(days => (rs_source.day - 1))) AS date)
      )
""", nativeQuery = true)
    List<Route> findRoutesWithAvailabilityOnDate(@Param("sourceStation") String sourceStation,
                                                 @Param("destinationStation") String destinationStation,
                                                 @Param("travelDate") LocalDate travelDate);






//    @Query("SELECT r FROM Route r " +
//            "JOIN r.stations s1 " +
//            "JOIN r.stations s2 " +
//            "WHERE s1.station.name = :sourceStation " +
//            "AND s2.station.name = :destinationStation " +
//            "AND FUNCTION('DATE_ADD', r.train.startDate, INTERVAL s1.day-1 DAY) = :travelDate " +
//            "AND EXISTS (SELECT sa FROM SeatAvailability sa " +
//            "WHERE sa.trainId = r.train.id " +
//            "AND sa.travelDate = :travelDate)")
//    List<Route> findTrainsBetweenStationsOnDate(@Param("sourceStation") String sourceStation,
//                                                @Param("destinationStation") String destinationStation,
//                                                @Param("travelDate") LocalDate travelDate);



    // Find a route by the associated train ID
    Optional<Route> findByTrainId(Long trainId);

    @Query("SELECT r FROM Route r JOIN r.train t WHERE t.trainNumber = :trainNumber")
    Optional<Route> findByTrainNumber(@Param("trainNumber") String trainNumber);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Route r JOIN r.train t " +
            "WHERE t.trainNumber = :trainNumber AND r.id = :routeId")
    boolean existsByTrainNumberAndRouteId(@Param("trainNumber") String trainNumber, @Param("routeId") Long routeId);

    @Query("SELECT r FROM Route r " +
            "LEFT JOIN FETCH r.stations rs " +
            "LEFT JOIN FETCH rs.station " + // Fetch Station as well
            "WHERE r.id = :routeId")
    Optional<Route> findByIdWithStations(@Param("routeId") Long routeId);

}
