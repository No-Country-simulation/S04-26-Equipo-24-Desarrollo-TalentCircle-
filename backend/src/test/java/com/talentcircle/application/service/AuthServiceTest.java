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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private EncryptionService encryptionService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, encryptionService);
    }

    @Test
    void login_ShouldReturnLoginResponse_WhenCredentialsAreValid() {
        // Given
        User user = new User();
        user.setId("123");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole(User.Role.ADMIN);
        user.setActive(true);
        user.setPasswordHash(new BCryptPasswordEncoder(12).encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("123", "ADMIN")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("123")).thenReturn("refresh-token");

        AuthUseCase.LoginRequest request = new AuthUseCase.LoginRequest("test@example.com", "password123");

        // When
        AuthUseCase.LoginResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("123", response.user().id());
    }

    @Test
    void login_ShouldThrowException_WhenUserIsInactive() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setActive(false);
        user.setPasswordHash(new BCryptPasswordEncoder(12).encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        AuthUseCase.LoginRequest request = new AuthUseCase.LoginRequest("test@example.com", "password123");

        // When & Then
        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void createUser_ShouldCreateUser_WhenEmailDoesNotExist() {
        // Given
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuthUseCase.UserDto result = authService.createUser("new@example.com", "password123", "New User", "EDITOR");

        // Then
        assertNotNull(result);
        assertEquals("new@example.com", result.email());
        assertEquals("New User", result.fullName());
        assertEquals("EDITOR", result.role());
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        // Given
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class,
            () -> authService.createUser("existing@example.com", "password123", "User", "EDITOR"));
    }
}
