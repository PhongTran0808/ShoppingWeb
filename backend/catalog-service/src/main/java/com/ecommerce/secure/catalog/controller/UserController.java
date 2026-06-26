package com.ecommerce.secure.catalog.controller;
import com.ecommerce.secure.catalog.entity.User;
import com.ecommerce.secure.catalog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public void softDeleteUser(@PathVariable String id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        if (username == null || password == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Username and password are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        User user = userOpt.get();
        if (user.getIsActive() != null && !user.getIsActive()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Account is deactivated");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA3-512");
            byte[] hashedBytes = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String computedHash = java.util.HexFormat.of().formatHex(hashedBytes);

            if (!computedHash.equalsIgnoreCase(user.getPasswordHash())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String token = com.ecommerce.secure.shared.service.JwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole() : "ROLE_USER"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            response.put("user", userMap);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error during authentication");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String email = payload.get("email");
        String password = payload.get("password");

        if (username == null || email == null || password == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Username, email, and password are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (userRepository.findByUsername(username).isPresent()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Username is already taken");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA3-512");
            byte[] hashedBytes = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String passwordHash = java.util.HexFormat.of().formatHex(hashedBytes);

            User user = new User();
            user.setId(java.util.UUID.randomUUID().toString());
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setRole("ROLE_USER");
            user.setIsActive(true);

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error during registration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
