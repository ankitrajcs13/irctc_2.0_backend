package com.irctc2.admin.controller;

import com.irctc2.security.jwt.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PostMapping("/invalidate-all")
    public ResponseEntity<?> invalidateAllTokens() {
        Date now = new Date();
        tokenBlacklistService.blacklistAllTokensBefore(now);
        return ResponseEntity.ok("All tokens invalidated");
    }
}
