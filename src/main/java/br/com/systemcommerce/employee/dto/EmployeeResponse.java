package br.com.systemcommerce.employee.dto;

import br.com.systemcommerce.employee.entity.Employee;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID organizationId,
        String organizationCode,
        String registrationNumber,
        String name,
        String socialName,
        String cpf,
        String rg,
        LocalDate birthDate,
        String email,
        String phone,
        String mobile,
        LocalDate admissionDate,
        LocalDate terminationDate,
        String jobTitle,
        Employee.EmployeeStatus status,
        UUID userId,
        String userLogin,
        boolean canSell,
        String notes,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
