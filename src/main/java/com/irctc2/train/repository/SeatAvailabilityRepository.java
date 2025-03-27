package com.irctc2.train.repository;

import com.irctc2.train.model.SeatAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeatAvailabilityRepository extends JpaRepository<SeatAvailability, Long> {


    @Query("SELECT sa FROM SeatAvailability sa " +
            "WHERE sa.trainId = :trainId " +
            "AND sa.travelDate = :travelDate " +
            "AND sa.bogie.bogieType = :bogieType")
    List<SeatAvailability> findByTrainIdAndTravelDateAndBogieType(@Param("trainId") Long trainId,
                                                                  @Param("travelDate") LocalDate travelDate,
                                                                  @Param("bogieType") String bogieType);

    boolean existsByTrainIdAndTravelDate(Long trainId, LocalDate travelDate);

}
