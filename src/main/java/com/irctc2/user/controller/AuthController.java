package com.irctc2.user.controller;

import com.irctc2.security.jwt.JwtTokenProvider;
import com.irctc2.user.dto.UserDTO;
import com.irctc2.user.model.User;
import com.irctc2.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody User loginRequest) {
        try {
            // Check if the user exists and is active
            Optional<UserDTO> userOptional = userService.getUserByEmail(loginRequest.getEmail());
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "User not found or has been deleted",
                        "details", "Please check your email or register again."
                ));
            }
            UserDTO user = userOptional.get();
            if ("Deactivated".equals(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "message", "User account has been deactivated",
                        "details", "Please contact support for further assistance."
                ));
            }
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // Set authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate a JWT token
            String accessToken = tokenProvider.generateToken(loginRequest.getEmail(), user.getRole());
            String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getEmail());

            // Return the token
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));

        } catch (UsernameNotFoundException ex) {
            // Handle case where the user does not exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", "User not found or has been deleted",
                    "details", "Please check your email or register again."
            ));
        } catch (BadCredentialsException ex) {
            // Handle case where the credentials are invalid
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Invalid credentials",
                    "details", "Please check your email and password."
            ));
        } catch (Exception ex) {
            // Handle other unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "An unexpected error occurred",
                    "details", ex.getMessage()
            ));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Refresh token is required"
            ));
        }

        try {
            // Validate the refresh token
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            if (username == null || !tokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "message", "Invalid or expired refresh token"
                ));
            }

            // Generate a new access token using the username
            Optional<UserDTO> userOptional = userService.getUserByEmail(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "User not found"
                ));
            }

            UserDTO user = userOptional.get();
            String newAccessToken = tokenProvider.generateToken(username, user.getRole());

            // Return the new access token
            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "An error occurred while refreshing the token",
                    "details", ex.getMessage()
            ));
        }
    }

}
