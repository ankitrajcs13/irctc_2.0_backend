package com.irctc2.user.service;

import com.irctc2.user.dto.UpdateRequest;
import com.irctc2.user.dto.UserDTO;
import com.irctc2.user.model.User;
import com.irctc2.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Value("${user.password.expiry-days}")
    private int passwordExpiryDays;


    public boolean isEmailTaken(String email) {
        // Check if a user with the same email already exists in the database
        return userRepository.existsByEmail(email);
    }


    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);  // Check if the username exists
    }

    public boolean isPhoneNumberTaken(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);  // Check if the phone number exists
    }
    public void saveUser(User user, String clientIp) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, passwordExpiryDays);
        user.setPasswordExpireDate(calendar.getTime());
        user.setUpdateIp(clientIp);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                        user.getLastName(), user.getPhoneNumber(), user.getGender(), user.getDob(),
                        user.getAddress(), user.getPincode(), user.getNationality(), user.getRole(), user.getStatus(),
                        user.getProfileImageUrl(), user.getIsVerified(), user.getPasswordExpireDate(),
                        user.getCreatedAt(), user.getUpdatedAt()));
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public boolean authenticateUser(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        System.out.println(email);

        // Check if the user exists and if the passwords match
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return passwordEncoder.matches(password, user.getPassword());
        }

        return false;
    }

    public Optional<User> updateUser(Long userId, UpdateRequest request) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Update fields only if they are provided in the request
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }
            if (request.getAddress() != null) {
                user.setAddress(request.getAddress());
            }
            if (request.getStatus() != null) {
                user.setStatus(request.getStatus());
            }
            if (request.getIsVerified() != null) {
                user.setIsVerified(request.getIsVerified());
            }

            // Save the updated user
            return Optional.of(userRepository.save(user));
        }

        return Optional.empty();
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean deleteUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            return true;
        }
        return false;
    }


    public boolean changePassword(Long id, String oldPassword, String newPassword) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public Page<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,Sort.by(Sort.Order.asc("id")));
        Page<User> usersPage = userRepository.findAll(pageable);

        return usersPage.map(user -> new UserDTO(
                user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getPhoneNumber(), user.getGender(), user.getDob(),
                user.getAddress(), user.getPincode(), user.getNationality(), user.getRole(),
                user.getStatus(), user.getProfileImageUrl(), user.getIsVerified(),
                user.getPasswordExpireDate(), user.getCreatedAt(), user.getUpdatedAt()));
    }


    public boolean verifyUser(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setStatus("Verified"); // Update status or add a `verified` flag
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean deactivateUser(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setStatus("Deactivated");
            userRepository.save(user);
            return true;
        }
        return false;
    }

}