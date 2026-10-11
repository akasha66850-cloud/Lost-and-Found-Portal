package com.example.lostfound.controller;

import com.example.lostfound.model.User;
import com.example.lostfound.repository.UserRepository;
import com.example.lostfound.security.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtUtil;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtTokenUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email    = body.get("email");
        String password = body.get("password");
        String fullName = body.getOrDefault("fullName", "");

        if (username == null || username.isBlank())
            return ResponseEntity.badRequest().body("Username is required");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body("Email is required");
        if (password == null || password.length() < 6)
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");

        if (userRepo.existsByUsername(username))
            return ResponseEntity.badRequest().body("username.exists");
        if (userRepo.existsByEmail(email))
            return ResponseEntity.badRequest().body("email.exists");

        User u = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role("USER")
                .build();
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("msg", "registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        var opt = userRepo.findByUsername(username);
        if (opt.isEmpty()) return ResponseEntity.status(401).body("invalid.credentials");

        var user = opt.get();
        if (!passwordEncoder.matches(password, user.getPassword()))
            return ResponseEntity.status(401).body("invalid.credentials");

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(Map.of(
                "token",    token,
                "role",     user.getRole(),
                "username", user.getUsername(),
                "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).body("Not authenticated");
        User user = userRepo.findByUsername(auth.getName())
                .orElse(null);
        if (user == null) return ResponseEntity.status(404).body("User not found");
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                "email",    user.getEmail(),
                "role",     user.getRole()
        ));
    }
}