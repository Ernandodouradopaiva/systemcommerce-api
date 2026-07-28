package br.com.systemcommerce.employee.dto;

import br.com.systemcommerce.employee.entity.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EmployeeUpdateRequest(
        @NotBlank(message = "matrícula é obrigatória") @Size(max = 40) String registrationNumber,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 200) String socialName,
        @Size(max = 14) String cpf,
        @Size(max = 30) String rg,
        LocalDate birthDate,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 30) String mobile,
        LocalDate admissionDate,
        LocalDate terminationDate,
        @Size(max = 120) String jobTitle,
        Employee.EmployeeStatus status,
        Boolean canSell,
        @Size(max = 2000) String notes) {}
