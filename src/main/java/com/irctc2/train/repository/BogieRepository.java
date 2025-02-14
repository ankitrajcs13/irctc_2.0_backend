package com.irctc2.train.repository;

import com.irctc2.train.model.Bogie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BogieRepository extends JpaRepository<Bogie, Long> {

    // Fetch all distinct bogie types for a given train
    @Query("SELECT DISTINCT b.bogieType FROM Bogie b WHERE b.train.id = :trainId")
    List<String> findDistinctBogieTypesByTrainId(@Param("trainId") Long trainId);
}
