package br.com.systemcommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank(message = "refreshToken é obrigatório") String refreshToken) {}
