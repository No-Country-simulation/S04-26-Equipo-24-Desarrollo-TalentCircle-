package com.talentcircle.application.service;

import com.talentcircle.common.security.EncryptionService;
import com.talentcircle.common.security.JwtService;
import com.talentcircle.domain.model.User;
import com.talentcircle.domain.port.in.AuthUseCase;
import com.talentcircle.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests for Auth flow.
 * Tests the complete login → refresh → logout cycle.
 * Tarea 15.1: Tests de integración para Auth.
 */
@ExtendWith(MockitoExtension.class)
class AuthIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    private JwtService jwtService;
    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-minimum-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 28800000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);

        authService = new AuthService(userRepository, jwtService, encryptionService);
        passwordEncoder = new BCryptPasswordEncoder(12);
    }

    @Test
    void authFlow_loginRefreshLogout_completeCycle() {
        // Setup user
        User user = buildUser("user-1", "admin@test.com", "password123", User.Role.ADMIN);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        // Step 1: Login
        AuthUseCase.LoginResponse loginResponse = authService.login(
                new AuthUseCase.LoginRequest("admin@test.com", "password123"));

        assertNotNull(loginResponse.accessToken());
        assertNotNull(loginResponse.refreshToken());
        assertEquals("user-1", loginResponse.user().id());
        assertEquals("ADMIN", loginResponse.user().role());

        // Verify access token claims
        assertTrue(jwtService.isValid(loginResponse.accessToken()));
        assertEquals("user-1", jwtService.extractUserId(loginResponse.accessToken()));
        assertEquals("ADMIN", jwtService.extractRole(loginResponse.accessToken()));

        // Step 2: Refresh
        AuthUseCase.LoginResponse refreshResponse = authService.refresh(
                new AuthUseCase.RefreshRequest(loginResponse.refreshToken()));

        assertNotNull(refreshResponse.accessToken());
        assertNotNull(refreshResponse.refreshToken());
        // New tokens should be valid
        assertTrue(jwtService.isValid(refreshResponse.accessToken()));

        // Step 3: Logout (idempotent)
        assertDoesNotThrow(() -> authService.logout("user-1"));
        assertDoesNotThrow(() -> authService.logout("user-1")); // idempotent
    }

    @Test
    void login_withInvalidPassword_throwsException() {
        User user = buildUser("user-1", "admin@test.com", "correctPassword", User.Role.ADMIN);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () ->
                authService.login(new AuthUseCase.LoginRequest("admin@test.com", "wrongPassword")));
    }

    @Test
    void login_withNonExistentUser_throwsException() {
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                authService.login(new AuthUseCase.LoginRequest("nonexistent@test.com", "password")));
    }

    @Test
    void refresh_withInvalidToken_throwsException() {
        assertThrows(RuntimeException.class, () ->
                authService.refresh(new AuthUseCase.RefreshRequest("invalid.token.here")));
    }

    @Test
    void createUser_thenLogin_fullCycle() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("new-user-id");
            return u;
        });

        AuthUseCase.UserDto created = authService.createUser("new@test.com", "pass123", "New User", "EDITOR");
        assertEquals("new@test.com", created.email());
        assertEquals("EDITOR", created.role());

        // Now login with the created user
        User savedUser = buildUser("new-user-id", "new@test.com", "pass123", User.Role.EDITOR);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(savedUser));

        AuthUseCase.LoginResponse loginResponse = authService.login(
                new AuthUseCase.LoginRequest("new@test.com", "pass123"));

        assertNotNull(loginResponse.accessToken());
        assertEquals("EDITOR", jwtService.extractRole(loginResponse.accessToken()));
    }

    private User buildUser(String id, String email, String rawPassword, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName("Test User");
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
