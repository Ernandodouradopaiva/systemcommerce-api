package br.com.systemcommerce.employee.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
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
@Table(name = "employee_store_assignments")
public class EmployeeStoreAssignment extends AuditableEntity {

    public enum AssignmentType {
        PERMANENT,
        TEMPORARY,
        SUPPORT,
        SUBSTITUTE
    }

    public enum AssignmentStatus {
        ACTIVE,
        ENDED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private AssignmentType assignmentType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "primary_assignment", nullable = false)
    private boolean primaryAssignment;

    @Column(name = "store_role", length = 120)
    private String storeRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "notes", length = 2000)
    private String notes;

    public boolean isActiveAssignment() {
        return status == AssignmentStatus.ACTIVE && Boolean.TRUE.equals(getActive());
    }

    public void end(LocalDate endDate) {
        this.endDate = endDate;
        this.status = AssignmentStatus.ENDED;
        this.primaryAssignment = false;
    }

    public boolean coversDate(LocalDate date) {
        if (date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        LocalDate thisEnd = endDate == null ? LocalDate.MAX : endDate;
        LocalDate thatEnd = otherEnd == null ? LocalDate.MAX : otherEnd;
        return !startDate.isAfter(thatEnd) && !otherStart.isAfter(thisEnd);
    }
}
