package com.irctc2.user.controller;

import com.irctc2.security.jwt.JwtTokenProvider;
import com.irctc2.user.dto.ChangePasswordRequest;
import com.irctc2.user.dto.LoginRequest;
import com.irctc2.user.dto.UpdateRequest;
import com.irctc2.user.model.User;
import com.irctc2.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user, HttpServletRequest request) {
        // Check if email already exists
        if (userService.isEmailTaken(user.getEmail())) {
            // Return 409 Conflict if email is already taken with a message string
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already registered. Please use a different email.");
        }

        String clientIp = request.getRemoteAddr();
        userService.saveUser(user, clientIp);  // Save the user (no need to return the user)

        // Return 201 Created status with a success message string
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User created successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginRequest loginRequest) {
        boolean isAuthenticated = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
        if (isAuthenticated) {
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateRequest request) {
        Optional<User> updatedUser = userService.updateUser(id, request);

        if (updatedUser.isPresent()) {
            return ResponseEntity.ok(updatedUser.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build(); // 204 No Content
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(HttpServletRequest request) {
        try {
            // Extract email from JWT token in the request
            String email = extractEmailFromRequest(request);

            // Use the email to delete the user
            if (userService.deleteUserByEmail(email)) {
                return ResponseEntity.noContent().build(); // 204 No Content
            } else {
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized if token is invalid
        }
    }

    private String extractEmailFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7); // Remove "Bearer " prefix
            if (tokenProvider.validateToken(token)) {
                return tokenProvider.getUsernameFromToken(token); // Extract email from the token
            }
        }
        throw new RuntimeException("Invalid or missing token");
    }



    @PatchMapping("/{id}/change-password")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        boolean isUpdated = userService.changePassword(id, request.getOldPassword(), request.getNewPassword());
        if (isUpdated) {
            return ResponseEntity.ok("Password updated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid old password");
        }
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> usersPage = userService.getAllUsers(page, size);
        return ResponseEntity.ok(usersPage.getContent());
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<String> verifyUser(@PathVariable Long id) {
        boolean isVerified = userService.verifyUser(id);
        if (isVerified) {
            return ResponseEntity.ok("User verified successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {
        boolean isDeactivated = userService.deactivateUser(id);
        if (isDeactivated) {
            return ResponseEntity.ok("User account deactivated successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

//    @GetMapping("/me")
//    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
//        String email = authentication.getName(); // Spring Security retrieves logged-in user's email
//        return userService.getUserByEmail(email)
//                .map(user -> ResponseEntity.ok(user))
//                .orElse(ResponseEntity.notFound().build());
//    }

}
