package br.com.systemcommerce.publicapi.dto;

public record PublicApiTokenResponse(String accessToken, String tokenType, long expiresIn, String scopes) {}
