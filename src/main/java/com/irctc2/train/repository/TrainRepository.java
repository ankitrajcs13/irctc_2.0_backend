package com.irctc2.train.repository;

import com.irctc2.train.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNumber(String trainNumber);
}
