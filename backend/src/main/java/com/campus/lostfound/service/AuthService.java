package com.campus.lostfound.service;

import com.campus.lostfound.dto.AuthResponse;
import com.campus.lostfound.dto.LoginRequest;
import com.campus.lostfound.dto.RegisterRequest;
import com.campus.lostfound.dto.UserDto;
import com.campus.lostfound.entity.Role;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.exception.DuplicateResourceException;
import com.campus.lostfound.exception.UnauthorizedException;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        if (request.getRollNumber() != null && !request.getRollNumber().isBlank()) {
            if (userRepository.existsByRollNumberIgnoreCase(request.getRollNumber().trim())) {
                throw new DuplicateResourceException("Roll number is already registered: " + request.getRollNumber());
            }
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .rollNumber(request.getRollNumber() != null && !request.getRollNumber().isBlank()
                        ? request.getRollNumber().trim().toUpperCase() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        auditService.log("USER", saved.getId(), "REGISTER", saved.getId(), "Student registered: " + saved.getEmail());

        return toDto(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByRollNumberIgnoreCase(identifier);
        }

        User user = userOpt.orElseThrow(() -> new UnauthorizedException("Invalid email/roll number or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email/roll number or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account has been deactivated. Please contact an administrator.");
        }

        String token = jwtTokenProvider.generateToken(user);
        long expiresAt = System.currentTimeMillis() + jwtTokenProvider.getExpirationMs();

        auditService.log("USER", user.getId(), "LOGIN", user.getId(), "User logged in: " + user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .expiresAt(expiresAt)
                .user(toDto(user))
                .build();
    }

    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .rollNumber(user.getRollNumber())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
