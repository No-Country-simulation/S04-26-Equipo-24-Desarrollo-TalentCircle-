package com.talentcircle.application.service;

import com.talentcircle.common.security.EncryptionService;
import com.talentcircle.common.security.JwtService;
import com.talentcircle.domain.model.User;
import com.talentcircle.domain.port.in.AuthUseCase;
import com.talentcircle.domain.port.out.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
        this.encryptionService = encryptionService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            throw new RuntimeException("User is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken();

        // Store refresh token hash in user entity (in real implementation, store in separate table)
        // For now, we'll just return it

        return new LoginResponse(
                accessToken,
                refreshToken,
                "28800000", // 8 hours in ms
                new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name())
        );
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        if (!jwtService.isValid(request.refreshToken())) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        // Extract userId from refresh token subject
        String userId = jwtService.extractUserId(request.refreshToken());
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found for refresh token");
        }
        User user = userOpt.get();
        if (!user.isActive()) {
            throw new RuntimeException("User is inactive");
        }
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken();
        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "28800000",
                new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name())
        );
    }

    @Override
    public void logout(String userId) {
        // In real implementation, revoke refresh token in DB
        // Mark token as revoked or delete it
    }

    @Override
    public UserDto createUser(String email, String password, String fullName, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(User.Role.valueOf(role));
        user.setActive(true);

        User saved = userRepository.save(user);

        return new UserDto(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole().name());
    }

    @Override
    public UserDto updateUser(String userId, String fullName, String role, Boolean active) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        if (fullName != null) {
            user.setFullName(fullName);
        }
        if (role != null) {
            user.setRole(User.Role.valueOf(role));
        }
        if (active != null) {
            user.setActive(active);
        }

        User saved = userRepository.save(user);

        return new UserDto(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole().name());
    }

    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
