package br.com.systemcommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Atualização cadastral. {@code roleCodes} é opcional: {@code null} mantém os grupos atuais;
 * quando informado, deve conter ao menos um código (grupos são preferencialmente geridos em /users/{id}/groups).
 */
public record UserUpdateRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 150) String name,
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido") String email,
        @NotBlank(message = "login é obrigatório") @Size(max = 100) String login,
        Set<String> roleCodes) {}
