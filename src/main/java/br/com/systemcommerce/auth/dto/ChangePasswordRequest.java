package br.com.systemcommerce.auth.dto;

import br.com.systemcommerce.security.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "senha atual é obrigatória") String currentPassword,
        @NotBlank(message = "nova senha é obrigatória") @StrongPassword String newPassword) {}
