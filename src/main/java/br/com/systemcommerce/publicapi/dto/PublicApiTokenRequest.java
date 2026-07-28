package br.com.systemcommerce.publicapi.dto;

import jakarta.validation.constraints.NotBlank;

public record PublicApiTokenRequest(
        @NotBlank String clientId, @NotBlank String clientSecret, String grantType) {}
