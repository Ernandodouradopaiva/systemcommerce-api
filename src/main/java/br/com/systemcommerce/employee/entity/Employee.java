package br.com.systemcommerce.employee.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employees")
public class Employee extends AuditableEntity {

    public enum EmployeeStatus {
        ACTIVE,
        INACTIVE,
        ON_LEAVE,
        TERMINATED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "registration_number", nullable = false, length = 40)
    private String registrationNumber;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "social_name", length = 200)
    private String socialName;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "rg", length = 30)
    private String rg;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "mobile", length = 30)
    private String mobile;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "can_sell", nullable = false)
    private boolean canSell = true;

    @Column(name = "notes", length = 2000)
    private String notes;

    public boolean isTerminated() {
        return status == EmployeeStatus.TERMINATED;
    }

    public boolean isOperationallyActive() {
        return Boolean.TRUE.equals(getActive()) && status == EmployeeStatus.ACTIVE;
    }

    public boolean canReceiveNewAssignment() {
        return !isTerminated() && Boolean.TRUE.equals(getActive());
    }
}
