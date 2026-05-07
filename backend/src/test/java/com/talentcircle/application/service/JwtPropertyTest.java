package com.talentcircle.application.service;

import com.talentcircle.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-Based Tests for JWT.
 * Propiedad 1: decode(generate(user)).role == user.role
 * RF-28: Autenticación JWT
 */
class JwtPropertyTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-minimum-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 28800000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);
    }

    /**
     * Propiedad 1: Round-trip de claims JWT.
     * Para cualquier userId y role, decode(generate(userId, role)).role == role
     */
    @Test
    void property1_jwtRoundTrip_roleIsPreserved() {
        String[] roles = {"ADMIN", "EDITOR"};
        String[] userIds = {"user-1", "user-abc-123", "550e8400-e29b-41d4-a716-446655440000"};

        for (String userId : userIds) {
            for (String role : roles) {
                String token = jwtService.generateAccessToken(userId, role);

                // Property: decoded role must equal original role
                assertEquals(role, jwtService.extractRole(token),
                        "Role round-trip failed for userId=" + userId + " role=" + role);

                // Property: decoded userId must equal original userId
                assertEquals(userId, jwtService.extractUserId(token),
                        "UserId round-trip failed for userId=" + userId);

                // Property: token must be valid immediately after generation
                assertTrue(jwtService.isValid(token),
                        "Token must be valid immediately after generation");

                // Property: token must not be expired immediately after generation
                assertFalse(jwtService.isExpired(token),
                        "Token must not be expired immediately after generation");
            }
        }
    }

    @Test
    void property1_jwtRoundTrip_invalidTokenIsRejected() {
        assertFalse(jwtService.isValid("invalid.token.here"));
        assertFalse(jwtService.isValid(""));
        assertFalse(jwtService.isValid(null));
    }
}
