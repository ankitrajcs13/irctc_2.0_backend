package com.irctc2.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class TokenBlacklistService {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    // Store blacklisted token in the database
    public void blacklistToken(String token, Date expiryDate) {
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiryDate(expiryDate);
        blacklistedTokenRepository.save(blacklistedToken);
    }

    // Check if a token is blacklisted in the database
    public boolean isTokenBlacklisted(String token) {
        Optional<BlacklistedToken> optionalToken = blacklistedTokenRepository.findByToken(token);

        if (optionalToken.isPresent()) {
            BlacklistedToken blacklistedToken = optionalToken.get();
            if (blacklistedToken.getExpiryDate().before(new Date())) {
                // Remove expired tokens from the database
                blacklistedTokenRepository.deleteByToken(token);
                return false;
            }
            return true;
        }

        return false;
    }

    // Optional method to remove all tokens before a certain timestamp
    public void blacklistAllTokensBefore(Date timestamp) {
        blacklistedTokenRepository.deleteAllByExpiryDateBefore(timestamp);
    }
}
