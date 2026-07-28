package br.com.systemcommerce.employee.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmployeeAssignmentEndRequest(
        @NotNull(message = "data de término é obrigatória") LocalDate endDate,
        @jakarta.validation.constraints.Size(max = 2000) String notes) {}
