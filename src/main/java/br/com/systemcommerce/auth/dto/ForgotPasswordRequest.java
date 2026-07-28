package br.com.systemcommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido") String email) {}
