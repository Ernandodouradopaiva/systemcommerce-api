package br.com.systemcommerce.employee.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EmployeeLinkUserRequest(@NotNull(message = "userId é obrigatório") UUID userId) {}
