package br.com.systemcommerce.employee.mapper;

import br.com.systemcommerce.employee.dto.EmployeeAssignmentResponse;
import br.com.systemcommerce.employee.dto.EmployeeCreateRequest;
import br.com.systemcommerce.employee.dto.EmployeeResponse;
import br.com.systemcommerce.employee.dto.EmployeeUpdateRequest;
import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getOrganization().getId(),
                employee.getOrganization().getCode(),
                employee.getRegistrationNumber(),
                employee.getName(),
                employee.getSocialName(),
                employee.getCpf(),
                employee.getRg(),
                employee.getBirthDate(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getMobile(),
                employee.getAdmissionDate(),
                employee.getTerminationDate(),
                employee.getJobTitle(),
                employee.getStatus(),
                employee.getUser() != null ? employee.getUser().getId() : null,
                employee.getUser() != null ? employee.getUser().getLogin() : null,
                employee.isCanSell(),
                employee.getNotes(),
                employee.getActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }

    public EmployeeAssignmentResponse toAssignmentResponse(EmployeeStoreAssignment assignment) {
        return new EmployeeAssignmentResponse(
                assignment.getId(),
                assignment.getEmployee().getId(),
                assignment.getStore().getId(),
                assignment.getStore().getCode(),
                assignment.getStore().getName(),
                assignment.getAssignmentType(),
                assignment.getStartDate(),
                assignment.getEndDate(),
                assignment.isPrimaryAssignment(),
                assignment.getStoreRole(),
                assignment.getStatus(),
                assignment.getNotes(),
                assignment.getActive(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    public void applyCreate(Employee employee, EmployeeCreateRequest request) {
        applyFields(
                employee,
                request.registrationNumber(),
                request.name(),
                request.socialName(),
                request.cpf(),
                request.rg(),
                request.birthDate(),
                request.email(),
                request.phone(),
                request.mobile(),
                request.admissionDate(),
                request.terminationDate(),
                request.jobTitle(),
                request.status() != null ? request.status() : Employee.EmployeeStatus.ACTIVE,
                request.canSell(),
                request.notes());
        employee.setActive(true);
    }

    public void applyUpdate(Employee employee, EmployeeUpdateRequest request) {
        applyFields(
                employee,
                request.registrationNumber(),
                request.name(),
                request.socialName(),
                request.cpf(),
                request.rg(),
                request.birthDate(),
                request.email(),
                request.phone(),
                request.mobile(),
                request.admissionDate(),
                request.terminationDate(),
                request.jobTitle(),
                request.status() != null ? request.status() : employee.getStatus(),
                request.canSell(),
                request.notes());
    }

    private void applyFields(
            Employee employee,
            String registrationNumber,
            String name,
            String socialName,
            String cpf,
            String rg,
            java.time.LocalDate birthDate,
            String email,
            String phone,
            String mobile,
            java.time.LocalDate admissionDate,
            java.time.LocalDate terminationDate,
            String jobTitle,
            Employee.EmployeeStatus status,
            Boolean canSell,
            String notes) {
        employee.setRegistrationNumber(
                MoneyAndQuantityUtils.requireText(registrationNumber, "Matrícula").toUpperCase());
        employee.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        employee.setSocialName(MoneyAndQuantityUtils.blankToNull(socialName));
        employee.setCpf(normalizeCpf(cpf));
        employee.setRg(MoneyAndQuantityUtils.blankToNull(rg));
        employee.setBirthDate(birthDate);
        employee.setEmail(MoneyAndQuantityUtils.blankToNull(email));
        employee.setPhone(MoneyAndQuantityUtils.blankToNull(phone));
        employee.setMobile(MoneyAndQuantityUtils.blankToNull(mobile));
        employee.setAdmissionDate(admissionDate);
        employee.setTerminationDate(terminationDate);
        employee.setJobTitle(MoneyAndQuantityUtils.blankToNull(jobTitle));
        employee.setStatus(status);
        if (canSell != null) {
            employee.setCanSell(canSell);
        }
        employee.setNotes(MoneyAndQuantityUtils.blankToNull(notes));
        if (status == Employee.EmployeeStatus.TERMINATED && terminationDate == null) {
            employee.setTerminationDate(java.time.LocalDate.now());
        }
        if (status == Employee.EmployeeStatus.INACTIVE || status == Employee.EmployeeStatus.TERMINATED) {
            employee.setActive(status != Employee.EmployeeStatus.TERMINATED ? employee.getActive() : false);
        }
        if (status == Employee.EmployeeStatus.ACTIVE) {
            employee.setActive(true);
        }
    }

    private static String normalizeCpf(String cpf) {
        if (!StringUtils.hasText(cpf)) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        return StringUtils.hasText(digits) ? digits : null;
    }
}
