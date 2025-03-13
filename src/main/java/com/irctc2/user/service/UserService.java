package com.irctc2.user.service;

import com.irctc2.user.dto.UpdateRequest;
import com.irctc2.user.model.User;
import com.irctc2.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public User saveUser(User user, String clientIp) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, passwordExpiryDays);
        user.setPasswordExpireDate(calendar.getTime());
        user.setUpdateIp(clientIp);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
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

    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
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