package br.com.systemcommerce.employee.dto;

import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeAssignmentUpdateRequest(
        @NotNull(message = "storeId é obrigatório") UUID storeId,
        @NotNull(message = "tipo de lotação é obrigatório") EmployeeStoreAssignment.AssignmentType assignmentType,
        @NotNull(message = "data de início é obrigatória") LocalDate startDate,
        LocalDate endDate,
        Boolean primaryAssignment,
        @Size(max = 120) String storeRole,
        @Size(max = 2000) String notes) {}
