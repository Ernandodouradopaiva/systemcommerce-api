package br.com.systemcommerce.user.dto;

import br.com.systemcommerce.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String login,
        User.UserStatus status,
        Boolean active,
        Integer failedLoginAttempts,
        Instant lastLoginAt,
        Instant lockedUntil,
        List<String> roles,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt) {}
