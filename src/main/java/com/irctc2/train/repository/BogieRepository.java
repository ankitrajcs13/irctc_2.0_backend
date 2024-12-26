package com.irctc2.train.repository;

import com.irctc2.train.model.Bogie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BogieRepository extends JpaRepository<Bogie, Long> {
}
