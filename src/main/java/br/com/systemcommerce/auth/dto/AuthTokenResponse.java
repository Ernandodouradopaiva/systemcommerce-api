package br.com.systemcommerce.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        AuthenticatedUserResponse user) {

    public record AuthenticatedUserResponse(
            UUID id, String name, String email, String login, List<String> roles, List<String> permissions) {}
}
