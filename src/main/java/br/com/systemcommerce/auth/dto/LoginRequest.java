package br.com.systemcommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "login ou e-mail é obrigatório") String username,
        @NotBlank(message = "senha é obrigatória") String password) {}
