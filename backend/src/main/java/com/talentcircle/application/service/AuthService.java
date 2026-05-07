package com.talentcircle.application.service;

import com.talentcircle.common.exception.ConflictException;
import com.talentcircle.common.exception.ForbiddenException;
import com.talentcircle.common.exception.ResourceNotFoundException;
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

        // Mismo mensaje para usuario no encontrado y contraseña incorrecta
        // evita enumerar usuarios válidos
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            throw new ForbiddenException("Credenciales inválidas");
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            throw new ForbiddenException("Usuario inactivo. Contacta al administrador.");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken();

        return new LoginResponse(
                accessToken,
                refreshToken,
                "28800000",
                new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name())
        );
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        if (!jwtService.isValid(request.refreshToken())) {
            throw new ForbiddenException("Refresh token inválido o expirado");
        }
        // TODO: implementar validación contra token almacenado en DB
        throw new ForbiddenException("Refresh token inválido o expirado");
    }

    @Override
    public void logout(String userId) {
        // TODO: revocar refresh token en DB
    }

    @Override
    public UserDto createUser(String email, String password, String fullName, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("El email ya está registrado");
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (fullName != null) user.setFullName(fullName);
        if (role != null)     user.setRole(User.Role.valueOf(role));
        if (active != null)   user.setActive(active);

        User saved = userRepository.save(user);

        return new UserDto(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole().name());
    }

    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ForbiddenException("Contraseña actual incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
