package br.com.systemcommerce.auth.dto;

import br.com.systemcommerce.security.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "token é obrigatório") String token,
        @NotBlank(message = "nova senha é obrigatória") @StrongPassword String newPassword) {}
