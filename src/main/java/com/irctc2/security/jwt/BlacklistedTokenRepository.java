package com.irctc2.security.jwt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    Optional<BlacklistedToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteAllByExpiryDateBefore(Date timestamp);
}
