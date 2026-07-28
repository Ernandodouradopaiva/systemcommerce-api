package br.com.systemcommerce.user.dto;

import br.com.systemcommerce.security.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 150) String name,
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido") String email,
        @NotBlank(message = "login é obrigatório") @Size(max = 100) String login,
        @NotBlank(message = "senha é obrigatória") @StrongPassword String password,
        @NotEmpty(message = "informe ao menos um perfil") Set<String> roleCodes) {}
