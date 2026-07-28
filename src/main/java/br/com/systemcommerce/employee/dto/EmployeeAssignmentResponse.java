package br.com.systemcommerce.employee.dto;

import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeAssignmentResponse(
        UUID id,
        UUID employeeId,
        UUID storeId,
        String storeCode,
        String storeName,
        EmployeeStoreAssignment.AssignmentType assignmentType,
        LocalDate startDate,
        LocalDate endDate,
        boolean primaryAssignment,
        String storeRole,
        EmployeeStoreAssignment.AssignmentStatus status,
        String notes,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
